package com.civdevops.petcam.domain.usecase.camera

import com.civdevops.petcam.domain.camera.CameraOperationResult
import com.civdevops.petcam.domain.repository.CameraRepository
import javax.inject.Inject

class SetTorchEnabledUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(enabled: Boolean): CameraOperationResult {
        return cameraRepository.setTorchEnabled(enabled)
    }
}