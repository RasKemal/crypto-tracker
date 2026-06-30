package com.example.stocktracker.ui.common

import androidx.compose.runtime.Immutable

@Immutable
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Success<T>(val data: T) : LoadState<T>
    data class Error(val message: String) : LoadState<Nothing>
}
