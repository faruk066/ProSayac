package com.prosayac.app.domain.repository

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun isUserLoggedIn(): Boolean
    fun getCurrentUserEmail(): String?
}
