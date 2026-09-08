package com.civdevops.petcam.feature.camera

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.FlashMode

@Composable
fun CameraRoute(
    previewStatus: CameraPreviewStatus,
    capabilities: CameraCapabilities?,
    canCapturePhoto: Boolean,
    onRequestCameraPermission: () -> Unit,
    onRequestCapturePermission: () -> Unit,
    onRetry: () -> Unit,
    onConfigurationChanged: (CameraLens, FlashMode) -> Unit,
    previewContent: @Composable BoxScope.() -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(capabilities) {
        viewModel.onCapabilitiesChanged(capabilities,)
    }

    val configuration = uiState.configuration

    LaunchedEffect(configuration) {
        if (configuration is CameraConfigurationState.Ready) {
            onConfigurationChanged(configuration.lens, configuration.flashMode,)
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
    )
}