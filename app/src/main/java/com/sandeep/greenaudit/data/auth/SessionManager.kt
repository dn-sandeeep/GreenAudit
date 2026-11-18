package com.sandeep.greenaudit.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists the logged-in flag (and the signed-in email) across app restarts. */
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val isLoggedIn: Flow<Boolean> = dataStore.data.map { it[IS_LOGGED_IN_KEY] ?: false }

    suspend fun onLoginSuccess(email: String) {
        dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN_KEY] = true
            prefs[EMAIL_KEY] = email
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        val EMAIL_KEY = stringPreferencesKey("email")
    }
}
