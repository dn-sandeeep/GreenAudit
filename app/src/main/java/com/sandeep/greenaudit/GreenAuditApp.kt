package com.sandeep.greenaudit

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.sandeep.greenaudit.di.ApplicationScope
import com.sandeep.greenaudit.domain.repository.ParkRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class GreenAuditApp : Application(), Configuration.Provider {

    @Inject
    lateinit var parkRepository: ParkRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { parkRepository.seedIfNeeded() }
    }
}
