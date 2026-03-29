package com.redcom1988.srwagent.screens.submissiondetail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.redcom1988.core.di.util.inject
import com.redcom1988.domain.submission.interactor.FinishPickup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubmissionDetailUiState(
    val isLoading: Boolean = false,
    val error: Throwable? = null,
    val isSuccess: Boolean = false
)

class SubmissionDetailScreenModel(
    private val finishPickup: FinishPickup = inject()
) : ScreenModel {

    private val _uiState = MutableStateFlow(SubmissionDetailUiState())
    val uiState: StateFlow<SubmissionDetailUiState> = _uiState.asStateFlow()

    fun completePickup(submissionId: Int, notes: String?) {
        screenModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                finishPickup(submissionId, notes)
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
