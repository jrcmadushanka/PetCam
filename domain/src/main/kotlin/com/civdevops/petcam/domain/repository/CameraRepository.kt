package com.civdevops.petcam.domain.repository

import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.RecordingState
import com.civdevops.petcam.domain.camera.CameraOperationResult
import com.civdevops.petcam.domain.camera.PhotoCaptureResult
import com.civdevops.petcam.domain.camera.RecordingCommandResult
import com.civdevops.petcam.domain.camera.VideoRecordingRequest
import com.civdevops.petcam.domain.camera.VideoRecordingResult
import kotlinx.coroutines.flow.Flow

interface CameraRepository {

    fun observeCapabilities(): Flow<CameraCapabilities>

    fun observeRecordingState(): Flow<RecordingState>

    suspend fun setLens(
        lens: CameraLens,
    ): CameraOperationResult

    suspend fun setFlashMode(
        flashMode: FlashMode,
    ): CameraOperationResult

    suspend fun capturePhoto(): PhotoCaptureResult

    suspend fun startVideoRecording(
        request: VideoRecordingRequest,
    ): RecordingCommandResult

    suspend fun pauseVideoRecording(): RecordingCommandResult

    suspend fun resumeVideoRecording(): RecordingCommandResult

    suspend fun stopVideoRecording(): VideoRecordingResult
}