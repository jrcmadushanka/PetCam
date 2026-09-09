package com.civdevops.petcam.domain.usecase.camera

import com.civdevops.petcam.domain.camera.RecordingCommandResult
import com.civdevops.petcam.domain.camera.VideoRecordingRequest
import com.civdevops.petcam.domain.repository.CameraRepository
import javax.inject.Inject

class StartVideoRecordingUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(request: VideoRecordingRequest): RecordingCommandResult {
        return cameraRepository.startVideoRecording(request)
    }
}