package com.sandeep.greenaudit.data.local.entity

/**
 * Where a row stands relative to the (fake) backend. Lives directly on
 * each syncable entity rather than in a separate outbox table - one
 * source of truth that both powers the "Pending sync" badge and drives
 * what SyncManager pushes. PENDING_DELETE is a tombstone: the row stays
 * until the backend confirms the delete, so deletions survive airplane
 * mode too.
 */
enum class SyncState {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
}
