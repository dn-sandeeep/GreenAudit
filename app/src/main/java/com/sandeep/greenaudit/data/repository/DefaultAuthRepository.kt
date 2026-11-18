package com.sandeep.greenaudit.data.repository

import com.sandeep.greenaudit.data.auth.SessionManager
import com.sandeep.greenaudit.data.local.dao.ParkDao
import com.sandeep.greenaudit.data.local.dao.PhotoDao
import com.sandeep.greenaudit.data.local.dao.SurveyDao
import com.sandeep.greenaudit.data.local.seed.SeedDataProvider
import com.sandeep.greenaudit.domain.auth.DemoCredentials
import com.sandeep.greenaudit.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultAuthRepository @Inject constructor(
    private val sessionManager: SessionManager,
    private val parkDao: ParkDao,
    private val surveyDao: SurveyDao,
    private val photoDao: PhotoDao,
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn

    override suspend fun login(email: String, password: String): Boolean {
        val valid = email.trim().equals(DemoCredentials.EMAIL, ignoreCase = true) &&
            password == DemoCredentials.PASSWORD
        if (valid) {
            sessionManager.onLoginSuccess(email.trim())
        }
        return valid
    }

    override suspend fun logout() {
        sessionManager.clear()
        // "Logout clears local data" - wipe the device copy, then reseed the
        // demo parks so the next login still has something to show.
        photoDao.clearAll()
        surveyDao.clearAll()
        parkDao.clearAll()
        SeedDataProvider.seedIfEmpty(parkDao)
    }
}
