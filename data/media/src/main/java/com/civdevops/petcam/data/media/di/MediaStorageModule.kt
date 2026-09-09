package com.civdevops.petcam.data.media.di

import com.civdevops.petcam.data.media.MediaStorePhotoStorage
import com.civdevops.petcam.data.media.MediaStoreVideoStorage
import com.civdevops.petcam.data.media.PhotoStorage
import com.civdevops.petcam.data.media.VideoStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaStorageModule {

    @Binds
    @Singleton
    abstract fun bindPhotoStorage(implementation: MediaStorePhotoStorage): PhotoStorage

    @Binds
    @Singleton
    abstract fun bindVideoStorage(implementation: MediaStoreVideoStorage): VideoStorage
}