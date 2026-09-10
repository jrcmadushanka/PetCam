package com.civdevops.petcam.feature.camera

import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.RecordingFailure
import com.civdevops.petcam.core.model.camera.RecordingState

data class CameraUiState(
    val configuration: CameraConfigurationState = CameraConfigurationState.Loading,
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val recordAudio: Boolean = true,
    val keepScreenAwakeWhileRecording: Boolean = true,
    val photoCapture: PhotoCaptureState = PhotoCaptureState.Idle,
    val recordingState: RecordingState = RecordingState.Idle,
    val videoCapture: VideoCaptureState = VideoCaptureState.Idle,
    val recordingCommandFailure: RecordingFailure? = null,
    val videoTorchEnabled: Boolean = false,
    val attentionSound: AttentionSoundUiState = AttentionSoundUiState()
) {
    val recordingInProgress: Boolean
        get() = when (recordingState) {
            RecordingState.Idle,
            is RecordingState.Failed -> false
            else -> true
        }
}