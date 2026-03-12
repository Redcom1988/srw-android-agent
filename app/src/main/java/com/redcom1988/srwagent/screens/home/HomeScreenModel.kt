package com.redcom1988.srwagent.screens.home

import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.redcom1988.core.di.util.inject
import com.redcom1988.domain.auth.interactor.Logout
import com.redcom1988.domain.submission.interactor.GetSubmissions
import com.redcom1988.domain.submission.model.Submission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoggingOut: Boolean = false,
    val logoutError: Throwable? = null
)

class HomeScreenModel(
    private val getSubmissions: GetSubmissions = inject(),
    private val logout: Logout = inject()
): ScreenModel {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val submissionsPagingData: Flow<PagingData<Submission>> = getSubmissions()
        .cachedIn(screenModelScope)

    fun logout() {
        screenModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoggingOut = true, logoutError = null)
            
            when (val result = logout.await()) {
                is Logout.Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoggingOut = false)
                }
                is Logout.Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggingOut = false,
                        logoutError = result.error
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(logoutError = null)
    }
}