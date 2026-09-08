package com.civdevops.petcam.data.camera

import android.content.Context
import androidx.annotation.MainThread
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraUnavailableException
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.InitializationException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CameraLensCapabilities
import com.civdevops.petcam.core.model.camera.FlashMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import java.lang.ref.WeakReference

@Singleton
class CameraXSession @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) {

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)

    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    private val _capabilities = MutableStateFlow<CameraCapabilities?>(null)

    val capabilities: StateFlow<CameraCapabilities?> = _capabilities.asStateFlow()

    private val previewUseCase = Preview.Builder().build().apply {
            setSurfaceProvider { request ->
                _surfaceRequest.value = request
            }
        }

    internal val imageCaptureUseCase = ImageCapture.Builder().setCaptureMode(
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
        ).build()

    internal var boundCamera: Camera? = null
        private set

    internal var boundLens: CameraLens? = null
        private set

    private var cameraProvider: ProcessCameraProvider? = null

    private var lifecycleOwnerRef: WeakReference<LifecycleOwner>? = null

    @MainThread
    suspend fun discoverCapabilities(): CameraCapabilities? {

        _capabilities.value?.let {
            return it
        }

        return try {
            val provider = obtainCameraProvider()

            val lenses = buildMap {
                CameraLens.entries.forEach { lens ->
                    val selector = lens.toCameraSelector()

                    if (provider.hasCamera(
                            selector,
                        )
                    ) {
                        val cameraInfo = provider.getCameraInfo(
                            selector,
                        )

                        put(
                            lens,
                            CameraLensCapabilities(
                                flashSupported = cameraInfo.hasFlashUnit(),

                                /*
                                 * Phase 5 populates this
                                 * from CameraX video
                                 * quality capabilities.
                                 */
                                supportedVideoQualities = emptySet(),
                            ),
                        )
                    }
                }
            }

            if (lenses.isEmpty()) {
                null
            } else {
                CameraCapabilities(
                    lenses = lenses,
                ).also {
                    _capabilities.value = it
                }
            }
        } catch (
            cancellation: CancellationException, ) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
    }

    @MainThread
    suspend fun bind(
        lifecycleOwner: LifecycleOwner,
        lens: CameraLens,
    ): CameraSessionBindResult {

        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            return CameraSessionBindResult.INVALID_STATE
        }

        return try {
            val provider = obtainCameraProvider()

            val selector = lens.toCameraSelector()

            if (!provider.hasCamera(selector)) {
                return CameraSessionBindResult.LENS_UNAVAILABLE
            }

            provider.unbind(
                previewUseCase,
                imageCaptureUseCase,
            )

            boundCamera = null
            boundLens = null
            _surfaceRequest.value = null

            /*
             * Safe baseline while rebinding.
             * The requested effective flash mode
             * is applied immediately afterwards.
             */
            imageCaptureUseCase.flashMode = ImageCapture.FLASH_MODE_OFF

            boundCamera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                previewUseCase,
                imageCaptureUseCase,
            )

            boundLens = lens
            lifecycleOwnerRef = WeakReference(lifecycleOwner)

            CameraSessionBindResult.SUCCESS
        } catch (
            cancellation: CancellationException, ) {
            throw cancellation
        } catch (
            _: InitializationException, ) {
            CameraSessionBindResult.CAMERA_UNAVAILABLE
        } catch (
            _: CameraUnavailableException, ) {
            CameraSessionBindResult.CAMERA_UNAVAILABLE
        } catch (
            _: CameraInfoUnavailableException, ) {
            CameraSessionBindResult.CAMERA_UNAVAILABLE
        } catch (
            _: SecurityException, ) {
            CameraSessionBindResult.CAMERA_UNAVAILABLE
        } catch (
            _: IllegalArgumentException, ) {
            CameraSessionBindResult.LENS_UNAVAILABLE
        } catch (
            _: IllegalStateException, ) {
            CameraSessionBindResult.INVALID_STATE
        } catch (
            _: Exception, ) {
            CameraSessionBindResult.UNKNOWN
        }
    }

    @MainThread
    fun setFlashMode(
        flashMode: FlashMode,
    ): CameraControlResult {

        val camera = boundCamera ?: return CameraControlResult.INVALID_STATE

        if (flashMode != FlashMode.OFF && !camera.cameraInfo.hasFlashUnit()) {
            imageCaptureUseCase.flashMode = ImageCapture.FLASH_MODE_OFF

            return CameraControlResult.FLASH_UNAVAILABLE
        }

        return try {
            imageCaptureUseCase.flashMode = flashMode.toImageCaptureMode()

            CameraControlResult.SUCCESS
        } catch (_: Exception) {
            CameraControlResult.UNKNOWN
        }
    }

    @MainThread
    fun focusAt(
        surfaceX: Float,
        surfaceY: Float,
        surfaceWidth: Float,
        surfaceHeight: Float,
    ): CameraControlResult {

        val camera = boundCamera ?: return CameraControlResult.INVALID_STATE

        if (surfaceWidth <= 0f || surfaceHeight <= 0f || !surfaceX.isFinite() || !surfaceY.isFinite()) {
            return CameraControlResult.INVALID_STATE
        }

        return try {
            val factory = SurfaceOrientedMeteringPointFactory(
                surfaceWidth,
                surfaceHeight,
            )

            val point = factory.createPoint(
                surfaceX.coerceIn(
                    0f,
                    surfaceWidth,
                ),
                surfaceY.coerceIn(
                    0f,
                    surfaceHeight,
                ),
            )

            val action = FocusMeteringAction.Builder(
                    point,
                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
                ).setAutoCancelDuration(
                    3,
                    TimeUnit.SECONDS,
                ).build()

            if (!camera.cameraInfo.isFocusMeteringSupported(
                        action,
                    )
            ) {
                return CameraControlResult.FOCUS_UNSUPPORTED
            }

            camera.cameraControl.startFocusAndMetering(
                    action,
                )

            CameraControlResult.SUCCESS
        } catch (_: Exception) {
            CameraControlResult.UNKNOWN
        }
    }

    @MainThread
    fun updateTargetRotation(
        rotation: Int,
    ) {
        previewUseCase.targetRotation = rotation

        imageCaptureUseCase.targetRotation = rotation
    }

    @MainThread
    fun unbind() {
        cameraProvider?.unbind(
            previewUseCase,
            imageCaptureUseCase,
        )

        boundCamera = null
        boundLens = null
        _surfaceRequest.value = null
        lifecycleOwnerRef = null
    }

    @MainThread
    suspend fun switchLens(lens: CameraLens): CameraSessionBindResult {
        val lifecycleOwner = lifecycleOwnerRef?.get() ?: return CameraSessionBindResult.INVALID_STATE

        return bind(lifecycleOwner, lens)
    }

    private suspend fun obtainCameraProvider(): ProcessCameraProvider =
        cameraProvider ?: ProcessCameraProvider.awaitInstance(
                applicationContext,
            ).also {
                cameraProvider = it
            }

    private fun CameraLens.toCameraSelector(): CameraSelector = when (this) {
        CameraLens.BACK -> CameraSelector.DEFAULT_BACK_CAMERA

        CameraLens.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
    }

    private fun FlashMode.toImageCaptureMode(): Int = when (this) {
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF

        FlashMode.ON -> ImageCapture.FLASH_MODE_ON

        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }
}