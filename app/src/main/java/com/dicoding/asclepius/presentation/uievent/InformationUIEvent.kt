package com.dicoding.asclepius.presentation.uievent

import com.dicoding.asclepius.domain.common.StringRes

sealed class InformationUIEvent {
    data class FailedLoadingInitialData(val message:StringRes):InformationUIEvent()
    data class SuccessLoadingInitialData(val message:StringRes? = null):InformationUIEvent()
}