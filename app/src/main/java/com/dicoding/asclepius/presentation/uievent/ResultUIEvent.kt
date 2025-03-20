package com.dicoding.asclepius.presentation.uievent

import com.dicoding.asclepius.presentation.utils.UIEvent

sealed class ResultUIEvent: UIEvent(){
    data object ShowSessionDialog:ResultUIEvent()
    data object SuccessSavingHistory:ResultUIEvent()
    data object FailedGettingProperExtras:ResultUIEvent()
}