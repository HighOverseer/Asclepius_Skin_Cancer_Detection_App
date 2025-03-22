package com.dicoding.asclepius.domain.common

sealed class StringRes {
    class Static(val resId: Int, vararg val args: Any) : StringRes()
    class Dynamic(val value: String) : StringRes()
}