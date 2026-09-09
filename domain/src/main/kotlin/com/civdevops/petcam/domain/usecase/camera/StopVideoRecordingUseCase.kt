package com.civdevops.petcam.domain.usecase.camera

import com.civdevops.petcam.domain.camera.VideoRecordingResult
import com.civdevops.petcam.domain.repository.CameraRepository
import javax.inject.Inject

class StopVideoRecordingUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(): VideoRecordingResult {
        return cameraRepository.stopVideoRecording()
    }
}