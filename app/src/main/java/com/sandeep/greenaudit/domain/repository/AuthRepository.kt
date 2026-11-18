package com.sandeep.greenaudit.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** True once a successful login has happened; survives app restart. */
    val isLoggedIn: Flow<Boolean>

    /** Validates against the single demo account; stores the session on success. */
    suspend fun login(email: String, password: String): Boolean

    /** Clears the session and wipes all local park/survey/photo data. */
    suspend fun logout()
}
