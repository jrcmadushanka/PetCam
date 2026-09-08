package com.civdevops.petcam.data.settings.di

import com.civdevops.petcam.data.settings.DefaultSettingsRepository
import com.civdevops.petcam.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        implementation: DefaultSettingsRepository,
    ): SettingsRepository
}