package com.dicoding.asclepius.domain.common

sealed class Resource<out T> private constructor() {
    data class Success<T>(val data: T, val message: StringRes? = null) : Resource<T>()
    data class Failure<T>(val message: StringRes, val data: T? = null) : Resource<T>()
    data class Error(val e: Exception) : Resource<Nothing>()
}