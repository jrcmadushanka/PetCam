package com.civdevops.petcam.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.data.camera.CameraControlResult
import com.civdevops.petcam.data.camera.CameraSessionBindResult
import com.civdevops.petcam.data.camera.CameraXSession
import com.civdevops.petcam.feature.camera.CameraPreviewStatus
import com.civdevops.petcam.feature.camera.CameraRoute

@Composable
fun CameraPreviewHost(
    cameraXSession: CameraXSession,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current

    val view = LocalView.current

    var hasCameraPermission by remember(context) {
        mutableStateOf(context.hasCameraPermission(),)
    }

    var canCapturePhoto by remember(context) {
        mutableStateOf(context.hasPhotoWriteAccess())
    }

    var bindFailed by remember {
        mutableStateOf(false)
    }

    var bindAttempt by remember {
        mutableIntStateOf(0)
    }

    var requestedLens by remember {
        mutableStateOf<CameraLens?>(
            null,
        )
    }

    var requestedFlashMode by remember { mutableStateOf(FlashMode.OFF) }

    var currentlyBoundLens by remember { mutableStateOf<CameraLens?>(null) }

    val surfaceRequest by cameraXSession.surfaceRequest.collectAsStateWithLifecycle()

    val capabilities by cameraXSession.capabilities.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    val capturePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        canCapturePhoto = context.hasPhotoWriteAccess()
    }

    DisposableEffect(
        lifecycleOwner,
        context,
    ) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = context.hasCameraPermission()
                canCapturePhoto = context.hasPhotoWriteAccess()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)

            cameraXSession.unbind()
        }
    }

    /*
     * Track display rotation independently from
     * Activity recreation/configuration changes.
     *
     * This also covers 180-degree rotations where
     * orientation classification might not change.
     */
    DisposableEffect(
        context,
        view,
        cameraXSession,
    ) {
        val displayManager = context.getSystemService(
            DisplayManager::class.java,
        )

        val listener = object : DisplayManager.DisplayListener {

            override fun onDisplayAdded(
                displayId: Int,
            ) = Unit

            override fun onDisplayRemoved(
                displayId: Int,
            ) = Unit

            override fun onDisplayChanged(
                displayId: Int,
            ) {
                val display = view.display

                if (display != null && display.displayId == displayId) {
                    cameraXSession.updateTargetRotation(
                        display.rotation,
                    )
                }
            }
        }

        displayManager.registerDisplayListener(
            listener,
            Handler(
                Looper.getMainLooper(),
            ),
        )

        cameraXSession.updateTargetRotation(
            view.display?.rotation ?: Surface.ROTATION_0,
        )

        onDispose {
            displayManager.unregisterDisplayListener(
                listener,
            )
        }
    }

    /*
     * Discover actual camera hardware only after
     * permission is available.
     */
    LaunchedEffect(
        hasCameraPermission,
        bindAttempt,
    ) {
        if (!hasCameraPermission) {
            bindFailed = false
            currentlyBoundLens = null

            cameraXSession.unbind()

            return@LaunchedEffect
        }

        val discovered = cameraXSession.discoverCapabilities()

        bindFailed = discovered == null
    }

    /*
     * Rebind only when the requested LENS changes.
     *
     * Flash changes do not need a complete camera
     * rebind.
     */
    LaunchedEffect(
        hasCameraPermission,
        lifecycleOwner,
        requestedLens,
        bindAttempt,
    ) {
        val lens = requestedLens ?: return@LaunchedEffect

        if (!hasCameraPermission) {
            return@LaunchedEffect
        }

        bindFailed = false

        cameraXSession.updateTargetRotation(
            view.display?.rotation ?: Surface.ROTATION_0,
        )

        when (cameraXSession.bind(
            lifecycleOwner = lifecycleOwner,
            lens = lens,
        )) {
            CameraSessionBindResult.SUCCESS -> {
                currentlyBoundLens = lens
            }

            else -> {
                currentlyBoundLens = null

                bindFailed = true
            }
        }
    }

    /*
     * Flash mode changes are cheap camera-control
     * updates and should not rebuild the session.
     */
    LaunchedEffect(
        hasCameraPermission,
        currentlyBoundLens,
        requestedFlashMode,
    ) {
        if (!hasCameraPermission || currentlyBoundLens == null) {
            return@LaunchedEffect
        }

        val result = cameraXSession.setFlashMode(
            requestedFlashMode,
        )

        if (result == CameraControlResult.FLASH_UNAVAILABLE) {
            cameraXSession.setFlashMode(
                FlashMode.OFF,
            )
        }
    }

    val previewStatus = when {
        !hasCameraPermission -> CameraPreviewStatus.PERMISSION_REQUIRED

        bindFailed -> CameraPreviewStatus.FAILED

        requestedLens == null -> CameraPreviewStatus.STARTING

        currentlyBoundLens != requestedLens -> CameraPreviewStatus.STARTING

        surfaceRequest == null -> CameraPreviewStatus.STARTING

        else -> CameraPreviewStatus.READY
    }

    CameraRoute(
        previewStatus = previewStatus,
        capabilities = capabilities,
        onRequestCameraPermission = {
            permissionLauncher.launch(
                Manifest.permission.CAMERA,
            )
        },
        onRetry = {
            bindAttempt++
        },
        onConfigurationChanged = {
                lens,
                flashMode,
            ->
            requestedLens = lens

            requestedFlashMode = flashMode
        },
        previewContent = {
            surfaceRequest?.let { request ->

                val coordinateTransformer = remember(request) {
                    MutableCoordinateTransformer()
                }

                CameraXViewfinder(
                    surfaceRequest = request,
                    coordinateTransformer = coordinateTransformer,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(
                            request,
                        ) {
                            detectTapGestures {
                                    offset,
                                ->
                                val surfaceOffset = with(
                                    coordinateTransformer,
                                ) {
                                    offset.transform()
                                }

                                cameraXSession.focusAt(
                                    surfaceX = surfaceOffset.x,
                                    surfaceY = surfaceOffset.y,
                                    surfaceWidth = request.resolution.width.toFloat(),
                                    surfaceHeight = request.resolution.height.toFloat(),
                                )
                            }
                        },
                )
            }
        },
        canCapturePhoto = canCapturePhoto,
        onRequestCapturePermission = {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                capturePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        })
}

private fun Context.hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(
    this,
    Manifest.permission.CAMERA,
) == PackageManager.PERMISSION_GRANTED

private fun Context.hasPhotoWriteAccess(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return true

    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}