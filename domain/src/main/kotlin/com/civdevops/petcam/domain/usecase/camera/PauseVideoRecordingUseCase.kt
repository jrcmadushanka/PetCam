package com.civdevops.petcam.domain.usecase.camera

import com.civdevops.petcam.domain.camera.RecordingCommandResult
import com.civdevops.petcam.domain.repository.CameraRepository
import javax.inject.Inject

class PauseVideoRecordingUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(): RecordingCommandResult {
        return cameraRepository.pauseVideoRecording()
    }
}