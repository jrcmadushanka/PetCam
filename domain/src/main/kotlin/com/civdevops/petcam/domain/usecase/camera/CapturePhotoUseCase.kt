package com.civdevops.petcam.domain.usecase.camera

import com.civdevops.petcam.domain.camera.PhotoCaptureResult
import com.civdevops.petcam.domain.repository.CameraRepository
import javax.inject.Inject

class CapturePhotoUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(): PhotoCaptureResult = cameraRepository.capturePhoto()
}