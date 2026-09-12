package com.civdevops.petcam.feature.camera

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.FlashMode
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CameraRoute(
    onOpenSettings: () -> Unit,
    previewStatus: CameraPreviewStatus,
    capabilities: CameraCapabilities?,
    canCapturePhoto: Boolean,
    microphonePermissionGranted: Boolean,
    onRequestCameraPermission: () -> Unit,
    onRequestCapturePermission: () -> Unit,
    onRequestMicrophonePermission: (onResult: (Boolean) -> Unit) -> Unit,
    onRetry: () -> Unit,
    onConfigurationChanged: (CameraLens, FlashMode) -> Unit,
    previewContent: @Composable BoxScope.() -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleStartEffect(viewModel) {
        viewModel.onAction(CameraAction.CameraActive)

        onStopOrDispose {
            viewModel.onAction(CameraAction.CameraInactive)
        }
    }

    LaunchedEffect(capabilities) {
        viewModel.onCapabilitiesChanged(capabilities)
    }

    LaunchedEffect(microphonePermissionGranted) {
        viewModel.onMicrophonePermissionChanged(microphonePermissionGranted)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                CameraEffect.RequestMicrophonePermission -> {
                    onRequestMicrophonePermission(viewModel::onMicrophonePermissionChanged)
                }
            }
        }
    }

    val configuration = uiState.configuration

    LaunchedEffect(configuration) {
        if (configuration is CameraConfigurationState.Ready) {
            onConfigurationChanged(configuration.lens, configuration.flashMode)
        }
    }

    CameraPreviewScreen(
        previewStatus = previewStatus,
        uiState = uiState,
        canCapturePhoto = canCapturePhoto,
        onAction = viewModel::onAction,
        onRequestCameraPermission = onRequestCameraPermission,
        onRequestCapturePermission = onRequestCapturePermission,
        onRetry = onRetry,
        previewContent = previewContent,
        onOpenSettings = onOpenSettings
    )
}