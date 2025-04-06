package com.example.sqlitepatient3.presentation.screens.diagnosis // Adjust package if needed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.domain.model.DiagnosticCode
import com.example.sqlitepatient3.domain.usecase.diagnosis.DeleteDiagnosticCodeUseCase
import com.example.sqlitepatient3.domain.usecase.diagnosis.GetAllDiagnosticCodesUseCase
import com.example.sqlitepatient3.domain.usecase.diagnosis.SearchDiagnosticCodesUseCase
import com.example.sqlitepatient3.domain.usecase.diagnosis.UpdateCommonCodeStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticCodeListViewModel @Inject constructor(
    private val getAllDiagnosticCodesUseCase: GetAllDiagnosticCodesUseCase,
    private val searchDiagnosticCodesUseCase: SearchDiagnosticCodesUseCase,
    private val deleteDiagnosticCodeUseCase: DeleteDiagnosticCodeUseCase,
    private val updateCommonCodeStatusUseCase: UpdateCommonCodeStatusUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null) // For snackbar messages
    val message: StateFlow<String?> = _message.asStateFlow()

    // Enum for sorting options (can be expanded later)
    enum class SortOption {
        CODE_ASC, CODE_DESC, DESCRIPTION_ASC, DESCRIPTION_DESC
    }
    private val _currentSortOption = MutableStateFlow(SortOption.CODE_ASC)
    val currentSortOption: StateFlow<SortOption> = _currentSortOption.asStateFlow()


    // Combine search, sort, and base data flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val diagnosticCodes: StateFlow<List<DiagnosticCode>> = combine(
        _searchQuery.debounce(300), // Add debounce for search
        _currentSortOption
    ) { query, sortOption ->
        Pair(query, sortOption) // Pass query and sort option together
    }.flatMapLatest { (query, sortOption) ->
        _isLoading.value = true
        val codesFlow = if (query.isBlank()) {
            getAllDiagnosticCodesUseCase()
        } else {
            searchDiagnosticCodesUseCase(query)
        }

        codesFlow.map { codes ->
            // Apply sorting
            val sortedCodes = when (sortOption) {
                SortOption.CODE_ASC -> codes.sortedBy { it.icdCode }
                SortOption.CODE_DESC -> codes.sortedByDescending { it.icdCode }
                SortOption.DESCRIPTION_ASC -> codes.sortedBy { it.description }
                SortOption.DESCRIPTION_DESC -> codes.sortedByDescending { it.description }
            }
            _isLoading.value = false
            sortedCodes
        }
    }.catch { e ->
        _isLoading.value = false
        _message.value = "Error loading codes: ${e.localizedMessage}"
        emit(emptyList()) // Emit empty list on error
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Actions ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _currentSortOption.value = option
    }

    fun deleteCode(code: DiagnosticCode) {
        viewModelScope.launch {
            try {
                deleteDiagnosticCodeUseCase(code)
                _message.value = "Code ${code.icdCode} deleted"
            } catch (e: Exception) {
                _message.value = "Error deleting code: ${e.localizedMessage}"
            }
        }
    }

    fun toggleCommonCode(code: DiagnosticCode) {
        viewModelScope.launch {
            try {
                // Current logic assumes commonCode is Int?, where 1 might mean common
                // Adjust this logic based on your actual implementation (e.g., Boolean field)
                val isCurrentlyCommon = code.commonCode != null && code.commonCode > 0 // Example check
                val newStatus = !isCurrentlyCommon
                // If using Boolean, pass newStatus directly. If Int?, pass 1 or null/0.
                // This example assumes you want to toggle based on current state and use Boolean for the use case
                val success = updateCommonCodeStatusUseCase(code.id, newStatus)
                if (!success) {
                    _message.value = "Failed to update common status for ${code.icdCode}"
                } else {
                    // Optionally show success message or rely on list refresh
                }
            } catch (e: Exception) {
                _message.value = "Error updating common status: ${e.localizedMessage}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}