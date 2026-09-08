package com.civdevops.petcam.data.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
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
import com.civdevops.petcam.core.model.share.QuickShareTarget

internal object SettingsPreferencesMapper {

    fun fromPreferences(
        preferences: Preferences,
    ): AppSettings =
        AppSettings(
            camera = cameraSettingsFrom(preferences),
            audio = audioSettingsFrom(preferences),
            sharing = sharingSettingsFrom(preferences),
            experience = experienceSettingsFrom(preferences),
        )

    fun writeCameraSettings(
        preferences: MutablePreferences,
        settings: CameraSettings,
    ) {
        preferences[SettingsPreferencesKeys.defaultCaptureMode] =
            settings.defaultMode.toPersistenceValue()

        preferences[SettingsPreferencesKeys.defaultCameraLens] =
            settings.defaultLens.toPersistenceValue()

        preferences[SettingsPreferencesKeys.flashMode] =
            settings.flashMode.toPersistenceValue()

        preferences[SettingsPreferencesKeys.videoQuality] =
            settings.videoQuality.toPersistenceValue()

        preferences[SettingsPreferencesKeys.recordAudio] =
            settings.recordAudio
    }

    fun writeAudioSettings(
        preferences: MutablePreferences,
        settings: AudioSettings,
    ) {
        preferences[SettingsPreferencesKeys.defaultSoundCategory] =
            settings.defaultCategory.rawValue

        preferences[SettingsPreferencesKeys.volumeMode] =
            settings.volumeMode.toPersistenceValue()

        preferences[SettingsPreferencesKeys.customVolumePercent] =
            settings.customVolumePercent

        preferences[SettingsPreferencesKeys.loopDuringRecording] =
            settings.loopDuringRecording

        preferences[SettingsPreferencesKeys.playOnPhotoCapture] =
            settings.playOnPhotoCapture
    }

    fun writeSharingSettings(
        preferences: MutablePreferences,
        settings: SharingSettings,
    ) {
        preferences[SettingsPreferencesKeys.autoOpenShareAfterCapture] =
            settings.autoOpenShareAfterCapture

        val preferredTarget =
            settings.preferredQuickShareTarget

        if (preferredTarget == null) {
            preferences.remove(
                SettingsPreferencesKeys.preferredQuickShareTarget,
            )
        } else {
            preferences[
                SettingsPreferencesKeys.preferredQuickShareTarget
            ] = preferredTarget.toPersistenceValue()
        }
    }

    fun writeExperienceSettings(
        preferences: MutablePreferences,
        settings: ExperienceSettings,
    ) {
        preferences[
            SettingsPreferencesKeys.keepScreenAwakeWhileRecording
        ] = settings.keepScreenAwakeWhileRecording

        preferences[SettingsPreferencesKeys.hapticsEnabled] =
            settings.hapticsEnabled

        preferences[SettingsPreferencesKeys.showOnlyAppMedia] =
            settings.showOnlyAppMedia

        preferences[SettingsPreferencesKeys.confirmDelete] =
            settings.confirmDelete
    }

    private fun cameraSettingsFrom(
        preferences: Preferences,
    ): CameraSettings =
        CameraSettings(
            defaultMode =
                captureModeFrom(
                    preferences[
                        SettingsPreferencesKeys.defaultCaptureMode
                    ],
                ),
            defaultLens =
                cameraLensFrom(
                    preferences[
                        SettingsPreferencesKeys.defaultCameraLens
                    ],
                ),
            flashMode =
                flashModeFrom(
                    preferences[
                        SettingsPreferencesKeys.flashMode
                    ],
                ),
            videoQuality =
                videoQualityFrom(
                    preferences[
                        SettingsPreferencesKeys.videoQuality
                    ],
                ),
            recordAudio =
                preferences[
                    SettingsPreferencesKeys.recordAudio
                ] ?: SettingsDefaults.camera.recordAudio,
        )

    private fun audioSettingsFrom(
        preferences: Preferences,
    ): AudioSettings {
        val category =
            preferences[
                SettingsPreferencesKeys.defaultSoundCategory
            ]
                ?.takeIf(String::isNotBlank)
                ?.let(::PetSoundCategory)
                ?: SettingsDefaults.audio.defaultCategory

        val customVolume =
            preferences[
                SettingsPreferencesKeys.customVolumePercent
            ]
                ?.takeIf { value ->
                    value in 0..100
                }
                ?: SettingsDefaults.audio.customVolumePercent

        return AudioSettings(
            defaultCategory = category,
            volumeMode =
                volumeModeFrom(
                    preferences[
                        SettingsPreferencesKeys.volumeMode
                    ],
                ),
            customVolumePercent = customVolume,
            loopDuringRecording =
                preferences[
                    SettingsPreferencesKeys.loopDuringRecording
                ] ?: SettingsDefaults.audio.loopDuringRecording,
            playOnPhotoCapture =
                preferences[
                    SettingsPreferencesKeys.playOnPhotoCapture
                ] ?: SettingsDefaults.audio.playOnPhotoCapture,
        )
    }

    private fun sharingSettingsFrom(
        preferences: Preferences,
    ): SharingSettings =
        SharingSettings(
            autoOpenShareAfterCapture =
                preferences[
                    SettingsPreferencesKeys.autoOpenShareAfterCapture
                ] ?: SettingsDefaults.sharing.autoOpenShareAfterCapture,
            preferredQuickShareTarget =
                quickShareTargetFrom(
                    preferences[
                        SettingsPreferencesKeys.preferredQuickShareTarget
                    ],
                ),
        )

    private fun experienceSettingsFrom(
        preferences: Preferences,
    ): ExperienceSettings =
        ExperienceSettings(
            keepScreenAwakeWhileRecording =
                preferences[
                    SettingsPreferencesKeys
                        .keepScreenAwakeWhileRecording
                ] ?: SettingsDefaults
                    .experience
                    .keepScreenAwakeWhileRecording,
            hapticsEnabled =
                preferences[
                    SettingsPreferencesKeys.hapticsEnabled
                ] ?: SettingsDefaults.experience.hapticsEnabled,
            showOnlyAppMedia =
                preferences[
                    SettingsPreferencesKeys.showOnlyAppMedia
                ] ?: SettingsDefaults.experience.showOnlyAppMedia,
            confirmDelete =
                preferences[
                    SettingsPreferencesKeys.confirmDelete
                ] ?: SettingsDefaults.experience.confirmDelete,
        )

    private fun CaptureMode.toPersistenceValue(): String =
        when (this) {
            CaptureMode.PHOTO -> "photo"
            CaptureMode.VIDEO -> "video"
        }

    private fun captureModeFrom(
        value: String?,
    ): CaptureMode =
        when (value) {
            "photo" -> CaptureMode.PHOTO
            "video" -> CaptureMode.VIDEO
            else -> SettingsDefaults.camera.defaultMode
        }

    private fun CameraLens.toPersistenceValue(): String =
        when (this) {
            CameraLens.BACK -> "back"
            CameraLens.FRONT -> "front"
        }

    private fun cameraLensFrom(
        value: String?,
    ): CameraLens =
        when (value) {
            "back" -> CameraLens.BACK
            "front" -> CameraLens.FRONT
            else -> SettingsDefaults.camera.defaultLens
        }

    private fun FlashMode.toPersistenceValue(): String =
        when (this) {
            FlashMode.OFF -> "off"
            FlashMode.ON -> "on"
            FlashMode.AUTO -> "auto"
        }

    private fun flashModeFrom(
        value: String?,
    ): FlashMode =
        when (value) {
            "off" -> FlashMode.OFF
            "on" -> FlashMode.ON
            "auto" -> FlashMode.AUTO
            else -> SettingsDefaults.camera.flashMode
        }

    private fun VideoQuality.toPersistenceValue(): String =
        when (this) {
            VideoQuality.UHD -> "uhd"
            VideoQuality.FHD -> "fhd"
            VideoQuality.HD -> "hd"
            VideoQuality.SD -> "sd"
        }

    private fun videoQualityFrom(
        value: String?,
    ): VideoQuality =
        when (value) {
            "uhd" -> VideoQuality.UHD
            "fhd" -> VideoQuality.FHD
            "hd" -> VideoQuality.HD
            "sd" -> VideoQuality.SD
            else -> SettingsDefaults.camera.videoQuality
        }

    private fun PetSoundVolumeMode.toPersistenceValue(): String =
        when (this) {
            PetSoundVolumeMode.FollowDevice ->
                "follow_device"

            PetSoundVolumeMode.MaximumAppOutput ->
                "maximum_app_output"

            PetSoundVolumeMode.Custom ->
                "custom"
        }

    private fun volumeModeFrom(
        value: String?,
    ): PetSoundVolumeMode =
        when (value) {
            "follow_device" ->
                PetSoundVolumeMode.FollowDevice

            "maximum_app_output" ->
                PetSoundVolumeMode.MaximumAppOutput

            "custom" ->
                PetSoundVolumeMode.Custom

            else ->
                SettingsDefaults.audio.volumeMode
        }

    private fun QuickShareTarget.toPersistenceValue(): String =
        when (this) {
            QuickShareTarget.TIKTOK -> "tiktok"
            QuickShareTarget.CAPCUT -> "capcut"
            QuickShareTarget.INSTAGRAM -> "instagram"
            QuickShareTarget.WHATSAPP -> "whatsapp"
        }

    private fun quickShareTargetFrom(
        value: String?,
    ): QuickShareTarget? =
        when (value) {
            "tiktok" -> QuickShareTarget.TIKTOK
            "capcut" -> QuickShareTarget.CAPCUT
            "instagram" -> QuickShareTarget.INSTAGRAM
            "whatsapp" -> QuickShareTarget.WHATSAPP
            else -> null
        }
}