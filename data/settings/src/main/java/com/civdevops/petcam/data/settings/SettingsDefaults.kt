package com.civdevops.petcam.data.settings

import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.VideoQuality
import com.civdevops.petcam.core.model.settings.AppSettings
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.core.model.settings.CameraSettings
import com.civdevops.petcam.core.model.settings.ExperienceSettings
import com.civdevops.petcam.core.model.settings.PetSoundVolumeMode
import com.civdevops.petcam.core.model.settings.SharingSettings

internal object SettingsDefaults {

    val camera = CameraSettings(
        defaultMode = CaptureMode.PHOTO,
        defaultLens = CameraLens.BACK,
        flashMode = FlashMode.OFF,
        videoQuality = VideoQuality.FHD,
        recordAudio = true,
    )

    val audio = AudioSettings(
        defaultCategory = PetSoundCategory("dogs"),
        volumeMode = PetSoundVolumeMode.FollowDevice,
        customVolumePercent = 75,
        loopDuringRecording = false,
        playOnPhotoCapture = true,
    )

    val sharing = SharingSettings(
        autoOpenShareAfterCapture = false,
        preferredQuickShareTarget = null,
    )

    val experience = ExperienceSettings(
        keepScreenAwakeWhileRecording = true,
        hapticsEnabled = true,
        showOnlyAppMedia = true,
        confirmDelete = true,
    )

    val app = AppSettings(
        camera = camera,
        audio = audio,
        sharing = sharing,
        experience = experience,
    )
}