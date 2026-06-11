package com.prosayac.app.domain.repository

interface SyncRepository {
    suspend fun fetchAndSaveAssignments(): Result<Unit>
}
