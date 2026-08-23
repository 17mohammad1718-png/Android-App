package com.dataguard.app.domain.util

/**
 * Generic result wrapper for operations that may fail.
 * Replaces ad-hoc error strings/booleans across ViewModels.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> default
    }

    fun errorMessage(): String? = when (this) {
        is Success -> null
        is Error -> exception.message ?: exception.javaClass.simpleName
    }

    companion object {
        /** Run [block] and wrap the result, catching any exception. */
        inline fun <T> runCatching(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: Exception) {
            Error(e)
        }
    }
}
