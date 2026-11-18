package com.sandeep.greenaudit.di

import android.content.Context
import androidx.room.Room
import com.sandeep.greenaudit.data.local.AppDatabase
import com.sandeep.greenaudit.data.local.dao.ParkDao
import com.sandeep.greenaudit.data.local.dao.PhotoDao
import com.sandeep.greenaudit.data.local.dao.SurveyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "greenaudit.db").build()

    @Provides
    fun provideParkDao(database: AppDatabase): ParkDao = database.parkDao()

    @Provides
    fun provideSurveyDao(database: AppDatabase): SurveyDao = database.surveyDao()

    @Provides
    fun providePhotoDao(database: AppDatabase): PhotoDao = database.photoDao()
}
