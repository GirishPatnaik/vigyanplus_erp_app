package com.vigyan.juniorcollege.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigyan.juniorcollege.data.local.entity.UserEntity
import com.vigyan.juniorcollege.data.repository.AuthRepository
import com.vigyan.juniorcollege.data.repository.LoginResult
import com.vigyan.juniorcollege.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedInUser: UserEntity? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onUsernameChange(value: String) { _state.value = _state.value.copy(username = value, error = null) }
    fun onPasswordChange(value: String) { _state.value = _state.value.copy(password = value, error = null) }

    fun login() {
        val current = _state.value
        if (current.username.isBlank() || current.password.isBlank()) {
            _state.value = current.copy(error = "Please enter both username and password")
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(isLoading = true, error = null)
            when (val result = authRepository.login(current.username, current.password)) {
                is LoginResult.Success -> {
                    sessionManager.login(result.user.id, result.user.fullName, result.user.role.name)
                    _state.value = _state.value.copy(isLoading = false, loggedInUser = result.user)
                }
                LoginResult.InvalidCredentials -> _state.value = _state.value.copy(
                    isLoading = false, error = "Invalid username or password"
                )
                LoginResult.AccountDisabled -> _state.value = _state.value.copy(
                    isLoading = false, error = "This account has been disabled"
                )
            }
        }
    }
}
