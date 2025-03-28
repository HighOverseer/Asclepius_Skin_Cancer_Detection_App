package com.dicoding.asclepius.presentation.uistate

import com.dicoding.asclepius.presentation.utils.ui.UIState

data class ResultUIState(
    val isLoading: Boolean = false,
    val isShowingDialog: Boolean = false
) : UIState()