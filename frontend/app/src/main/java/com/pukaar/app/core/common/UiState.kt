package com.pukaar.app.core.common

/**
 * The state a single screen exposes to Compose.
 *
 * Every ViewModel in [com.pukaar.app.ui.screen] holds one immutable snapshot
 * of this shape in a `StateFlow`, so a screen can be rendered from it alone.
 */
data class UiState<out T>(
    val isLoading: Boolean = false,
    val data: T? = null,
    val errorMessage: String? = null
) {
    val hasError: Boolean get() = errorMessage != null
}

fun <T> Resource<T>.toUiState(): UiState<T> = when (this) {
    is Resource.Loading -> UiState(isLoading = true)
    is Resource.Success -> UiState(data = data)
    is Resource.Error -> UiState(errorMessage = message)
}
