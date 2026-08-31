package com.pukaar.app.core.common

/**
 * A generic wrapper for a value produced by the data layer.
 *
 * Repositories and use cases return [Resource] so that callers handle success,
 * failure and the in-flight state explicitly instead of relying on exceptions.
 */
sealed interface Resource<out T> {

    data class Success<out T>(val data: T) : Resource<T>

    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : Resource<Nothing>

    data object Loading : Resource<Nothing>
}

inline fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error -> this
    is Resource.Loading -> this
}

fun <T> Resource<T>.dataOrNull(): T? = (this as? Resource.Success)?.data
