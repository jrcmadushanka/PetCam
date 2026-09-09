package com.civdevops.petcam.data.camera.di

import com.civdevops.petcam.data.camera.DefaultCameraRepository
import com.civdevops.petcam.domain.repository.CameraRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCameraRepository(implementation: DefaultCameraRepository): CameraRepository
}