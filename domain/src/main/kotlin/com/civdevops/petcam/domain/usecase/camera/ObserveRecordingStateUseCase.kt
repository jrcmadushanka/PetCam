package com.civdevops.petcam.domain.usecase.camera

import com.civdevops.petcam.core.model.camera.RecordingState
import com.civdevops.petcam.domain.repository.CameraRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveRecordingStateUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    operator fun invoke(): Flow<RecordingState> = cameraRepository.observeRecordingState()
}