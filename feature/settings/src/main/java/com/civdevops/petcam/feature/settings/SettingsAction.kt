package com.civdevops.petcam.feature.settings

import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.VideoQuality
import com.civdevops.petcam.core.model.settings.PetSoundVolumeMode
import com.civdevops.petcam.core.model.share.QuickShareTarget

sealed interface SettingsAction {

    data class SelectSection(
        val section: SettingsSection,
    ) : SettingsAction

    data object ShowSectionList : SettingsAction

    data object RetryLoad : SettingsAction

    data object DismissSaveError : SettingsAction

    data class SetDefaultCaptureMode(
        val mode: CaptureMode,
    ) : SettingsAction

    data class SetDefaultLens(
        val lens: CameraLens,
    ) : SettingsAction

    data class SetFlashMode(
        val mode: FlashMode,
    ) : SettingsAction

    data class SetVideoQuality(
        val quality: VideoQuality,
    ) : SettingsAction

    data class SetRecordAudio(
        val enabled: Boolean,
    ) : SettingsAction

    data class SetDefaultSoundCategory(
        val category: PetSoundCategory,
    ) : SettingsAction

    data class SetVolumeMode(
        val mode: PetSoundVolumeMode,
    ) : SettingsAction

    data class SetCustomVolume(
        val percent: Int,
    ) : SettingsAction

    data class SetLoopDuringRecording(
        val enabled: Boolean,
    ) : SettingsAction

    data class SetPlayOnPhotoCapture(
        val enabled: Boolean,
    ) : SettingsAction

    data class SetAutoOpenShareAfterCapture(
        val enabled: Boolean,
    ) : SettingsAction

    data class SetPreferredQuickShareTarget(
        val target: QuickShareTarget?,
    ) : SettingsAction

    data class SetKeepScreenAwake(
        val enabled: Boolean,
    ) : SettingsAction

    data class SetHapticsEnabled(
        val enabled: Boolean,
    ) : SettingsAction

    data class SetShowOnlyAppMedia(
        val enabled: Boolean,
    ) : SettingsAction

    data class SetConfirmDelete(
        val enabled: Boolean,
    ) : SettingsAction
}