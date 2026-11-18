package com.sandeep.greenaudit.di

import com.sandeep.greenaudit.data.remote.FakeSyncApi
import com.sandeep.greenaudit.data.remote.SyncApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindSyncApi(impl: FakeSyncApi): SyncApi
}
