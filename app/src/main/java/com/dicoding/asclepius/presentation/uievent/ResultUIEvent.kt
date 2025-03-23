package com.dicoding.asclepius.presentation.uievent

import com.dicoding.asclepius.presentation.utils.UIEvent
import com.dicoding.asclepius.presentation.view.ResultActivity

sealed class ResultUIEvent : UIEvent() {
    data object ShowSessionDialog : ResultUIEvent()
    data object SessionDialogCanceled: ResultUIEvent()
    data object SuccessSavingHistory : ResultUIEvent()
    data object FailedGettingProperExtras : ResultUIEvent()
}