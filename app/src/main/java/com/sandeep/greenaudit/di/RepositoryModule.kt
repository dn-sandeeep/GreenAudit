package com.sandeep.greenaudit.di

import com.sandeep.greenaudit.data.repository.DefaultAuthRepository
import com.sandeep.greenaudit.data.repository.DefaultParkRepository
import com.sandeep.greenaudit.data.repository.DefaultPhotoRepository
import com.sandeep.greenaudit.data.repository.DefaultSurveyRepository
import com.sandeep.greenaudit.domain.repository.AuthRepository
import com.sandeep.greenaudit.domain.repository.ParkRepository
import com.sandeep.greenaudit.domain.repository.PhotoRepository
import com.sandeep.greenaudit.domain.repository.SurveyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindParkRepository(impl: DefaultParkRepository): ParkRepository

    @Binds
    @Singleton
    abstract fun bindSurveyRepository(impl: DefaultSurveyRepository): SurveyRepository

    @Binds
    @Singleton
    abstract fun bindPhotoRepository(impl: DefaultPhotoRepository): PhotoRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository
}
