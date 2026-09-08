package com.civdevops.petcam.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.domain.usecase.camera.ResolveAvailableCameraLensUseCase
import com.civdevops.petcam.domain.usecase.camera.ResolveEffectiveFlashModeUseCase
import com.civdevops.petcam.domain.usecase.settings.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class CameraViewModel @Inject constructor(
    observeSettingsUseCase:
    ObserveSettingsUseCase,
    private val resolveAvailableCameraLensUseCase:
    ResolveAvailableCameraLensUseCase,
    private val resolveEffectiveFlashModeUseCase:
    ResolveEffectiveFlashModeUseCase,
) : ViewModel() {

    private val capabilities =
        MutableStateFlow<CameraCapabilities?>(
            null,
        )

    private val lensOverride =
        MutableStateFlow<CameraLens?>(
            null,
        )

    private val flashOverride =
        MutableStateFlow<FlashMode?>(
            null,
        )

    private val cameraSettings =
        observeSettingsUseCase()
            .map {
                it.camera
            }

    val uiState =
        combine(
            cameraSettings,
            capabilities,
            lensOverride,
            flashOverride,
        ) {
                settings,
                capabilitiesValue,
                lensOverrideValue,
                flashOverrideValue,
            ->

            if (capabilitiesValue == null) {
                return@combine CameraUiState()
            }

            val preferredLens =
                lensOverrideValue
                    ?: settings.defaultLens

            val effectiveLens =
                resolveAvailableCameraLensUseCase(
                    preferredLens,
                    capabilitiesValue,
                )

            val lensCapabilities =
                requireNotNull(
                    capabilitiesValue[
                        effectiveLens
                    ],
                )

            val preferredFlash =
                flashOverrideValue
                    ?: settings.flashMode

            val effectiveFlash =
                resolveEffectiveFlashModeUseCase(
                    preferredFlash,
                    lensCapabilities,
                )

            CameraUiState(
                configuration =
                    CameraConfigurationState.Ready(
                        capabilities =
                            capabilitiesValue,
                        lens =
                            effectiveLens,
                        flashMode =
                            effectiveFlash,
                    ),
            )
        }.stateIn(
            scope = viewModelScope,
            started =
                SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = 5_000,
                ),
            initialValue =
                CameraUiState(),
        )

    fun onCapabilitiesChanged(
        value: CameraCapabilities?,
    ) {
        capabilities.value = value
    }

    fun onAction(
        action: CameraAction,
    ) {
        when (action) {
            CameraAction.SwitchLens ->
                switchLens()

            CameraAction.CycleFlash ->
                cycleFlash()
        }
    }

    private fun switchLens() {
        val ready =
            uiState.value.configuration
                    as? CameraConfigurationState.Ready
                ?: return

        val requestedLens =
            when (ready.lens) {
                CameraLens.BACK ->
                    CameraLens.FRONT

                CameraLens.FRONT ->
                    CameraLens.BACK
            }

        if (
            ready.capabilities[
                requestedLens
            ] != null
        ) {
            lensOverride.value =
                requestedLens
        }
    }

    private fun cycleFlash() {
        val ready =
            uiState.value.configuration
                    as? CameraConfigurationState.Ready
                ?: return

        if (!ready.flashSupported) {
            flashOverride.value =
                FlashMode.OFF

            return
        }

        flashOverride.value =
            when (ready.flashMode) {
                FlashMode.OFF ->
                    FlashMode.ON

                FlashMode.ON ->
                    FlashMode.AUTO

                FlashMode.AUTO ->
                    FlashMode.OFF
            }
    }
}