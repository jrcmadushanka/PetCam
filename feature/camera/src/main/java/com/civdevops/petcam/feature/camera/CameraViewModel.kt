package com.civdevops.petcam.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.domain.camera.PhotoCaptureFailure
import com.civdevops.petcam.domain.camera.PhotoCaptureResult
import com.civdevops.petcam.domain.usecase.camera.CapturePhotoUseCase
import com.civdevops.petcam.domain.usecase.camera.ResolveAvailableCameraLensUseCase
import com.civdevops.petcam.domain.usecase.camera.ResolveEffectiveFlashModeUseCase
import com.civdevops.petcam.domain.usecase.settings.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val resolveAvailableCameraLensUseCase: ResolveAvailableCameraLensUseCase,
    private val resolveEffectiveFlashModeUseCase: ResolveEffectiveFlashModeUseCase,
    private val capturePhotoUseCase: CapturePhotoUseCase
) : ViewModel() {

    private val capabilities = MutableStateFlow<CameraCapabilities?>(null)
    private val lensOverride = MutableStateFlow<CameraLens?>(null)
    private val flashOverride = MutableStateFlow<FlashMode?>(null)
    private val photoCaptureState = MutableStateFlow<PhotoCaptureState>(PhotoCaptureState.Idle)

    private val cameraSettings = observeSettingsUseCase().map { it.camera }

    private val configurationState: Flow<CameraConfigurationState> =
        combine(cameraSettings, capabilities, lensOverride, flashOverride) {
                settings, capabilitiesValue, lensOverrideValue, flashOverrideValue ->

            if (capabilitiesValue == null) return@combine CameraConfigurationState.Loading

            val preferredLens = lensOverrideValue ?: settings.defaultLens
            val effectiveLens = resolveAvailableCameraLensUseCase(preferredLens, capabilitiesValue)
            val lensCapabilities = requireNotNull(capabilitiesValue[effectiveLens])
            val preferredFlash = flashOverrideValue ?: settings.flashMode
            val effectiveFlash = resolveEffectiveFlashModeUseCase(preferredFlash, lensCapabilities)

            CameraConfigurationState.Ready(
                capabilities = capabilitiesValue,
                lens = effectiveLens,
                flashMode = effectiveFlash
            )
        }

    val uiState = combine(configurationState, photoCaptureState) { configuration, photoCapture ->
        CameraUiState(configuration = configuration, photoCapture = photoCapture)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CameraUiState()
    )

    fun onCapabilitiesChanged(value: CameraCapabilities?) {
        capabilities.value = value
    }

    fun onAction(action: CameraAction) {
        when (action) {
            CameraAction.SwitchLens -> switchLens()
            CameraAction.CycleFlash -> cycleFlash()
            CameraAction.CapturePhoto -> capturePhoto()
        }
    }

    private fun switchLens() {
        val ready = uiState.value.configuration as? CameraConfigurationState.Ready ?: return
        val requestedLens = if (ready.lens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK

        if (ready.capabilities[requestedLens] != null) lensOverride.value = requestedLens
    }

    private fun cycleFlash() {
        val ready = uiState.value.configuration as? CameraConfigurationState.Ready ?: return

        if (!ready.flashSupported) {
            flashOverride.value = FlashMode.OFF
            return
        }

        flashOverride.value = when (ready.flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
    }

    private fun capturePhoto() {
        if (photoCaptureState.value == PhotoCaptureState.Capturing) return

        viewModelScope.launch {
            photoCaptureState.value = PhotoCaptureState.Capturing

            photoCaptureState.value = try {
                when (val result = capturePhotoUseCase()) {
                    is PhotoCaptureResult.Saved -> PhotoCaptureState.Saved(result.mediaId)
                    is PhotoCaptureResult.Failed -> PhotoCaptureState.Failed(result.failure)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                PhotoCaptureState.Failed(PhotoCaptureFailure.UNKNOWN)
            }
        }
    }
}