package com.sandeep.greenaudit.domain.auth

/**
 * The only account this mock-auth build accepts. Documented in the
 * README's "demo account" section per the task brief - there is no real
 * user database, so this is intentionally the single source of truth.
 */
object DemoCredentials {
    const val EMAIL = "surveyor@greenaudit.app"
    const val PASSWORD = "GreenAudit123"
}
