package com.civdevops.petcam.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object SettingsPreferencesKeys {

    // Camera
    val defaultCaptureMode =
        stringPreferencesKey("camera_default_capture_mode")

    val defaultCameraLens =
        stringPreferencesKey("camera_default_lens")

    val flashMode =
        stringPreferencesKey("camera_flash_mode")

    val videoQuality =
        stringPreferencesKey("camera_video_quality")

    val recordAudio =
        booleanPreferencesKey("camera_record_audio")

    // Audio
    val defaultSoundCategory =
        stringPreferencesKey("audio_default_sound_category")

    val volumeMode =
        stringPreferencesKey("audio_volume_mode")

    val customVolumePercent =
        intPreferencesKey("audio_custom_volume_percent")

    val loopDuringRecording =
        booleanPreferencesKey("audio_loop_during_recording")

    val playOnPhotoCapture =
        booleanPreferencesKey("audio_play_on_photo_capture")

    // Sharing
    val autoOpenShareAfterCapture =
        booleanPreferencesKey("sharing_auto_open_after_capture")

    val preferredQuickShareTarget =
        stringPreferencesKey("sharing_preferred_quick_target")

    // Experience
    val keepScreenAwakeWhileRecording =
        booleanPreferencesKey(
            "experience_keep_screen_awake_while_recording",
        )

    val hapticsEnabled =
        booleanPreferencesKey("experience_haptics_enabled")

    val showOnlyAppMedia =
        booleanPreferencesKey("experience_show_only_app_media")

    val confirmDelete =
        booleanPreferencesKey("experience_confirm_delete")
}