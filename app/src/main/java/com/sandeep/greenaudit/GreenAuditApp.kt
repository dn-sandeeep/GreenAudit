package com.sandeep.greenaudit

import android.app.Application
import com.sandeep.greenaudit.di.ApplicationScope
import com.sandeep.greenaudit.domain.repository.ParkRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class GreenAuditApp : Application() {

    @Inject
    lateinit var parkRepository: ParkRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { parkRepository.seedIfNeeded() }
    }
}
