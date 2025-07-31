package com.example.couchbaseassignment.manager

import com.example.couchbaseassignment.model.ErrorModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ErrorManager {
    private val _errors = MutableStateFlow<List<ErrorModel>>(emptyList())
    val errors: StateFlow<List<ErrorModel>> = _errors.asStateFlow()

    fun showError(error: ErrorModel) {
        _errors.value = _errors.value + error
    }

    fun dismissNext() {
        val currentErrors = _errors.value
        if (currentErrors.isNotEmpty()) {
            _errors.value = currentErrors.drop(1)
        }
    }
} 