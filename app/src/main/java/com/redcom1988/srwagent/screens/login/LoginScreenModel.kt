package com.redcom1988.srwagent.screens.login

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.redcom1988.core.di.util.inject
import com.redcom1988.domain.auth.interactor.Login
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LoginScreenModel(
    private val login: Login = inject()
) : ScreenModel {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun updateUsername(username: String) {
        _state.value = _state.value.copy(username = username)
    }

    fun updatePassword(password: String) {
        _state.value = _state.value.copy(password = password)
    }

    fun togglePasswordVisibility() {
        _state.value = _state.value.copy(isPasswordVisible = !_state.value.isPasswordVisible)
    }

    fun login() {
        val currentState = _state.value
        screenModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            when (val result = login.await(
                username = currentState.username,
                password = currentState.password
            )) {
                is Login.Result.Success -> {
                    _state.value = _state.value.copy(isLoading = false)
                }
                is Login.Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun resetState() {
        _state.value = _state.value.copy(isLoading = false, errorMessage = null)
    }
}
