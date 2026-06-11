package com.prosayac.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _loginSuccess = Channel<Unit>(Channel.BUFFERED)
    val loginSuccess = _loginSuccess.receiveAsFlow()

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.value = state.copy(error = "E-posta adresi gerekli")
            return
        }
        if (state.password.isBlank()) {
            _uiState.value = state.copy(error = "Şifre gerekli")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.login(state.email, state.password)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _loginSuccess.trySend(Unit)
                },
                onFailure = { e ->
                    val message = when {
                        e.message?.contains("Invalid login credentials") == true ->
                            "E-posta veya şifre hatalı"
                        e.message?.contains("Email not confirmed") == true ->
                            "E-posta adresi henüz doğrulanmamış"
                        else -> e.message ?: "Giriş yapılırken bir hata oluştu"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = message
                    )
                }
            )
        }
    }
}
