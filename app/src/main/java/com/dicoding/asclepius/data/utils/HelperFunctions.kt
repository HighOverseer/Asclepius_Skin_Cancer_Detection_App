package com.dicoding.asclepius.data.utils

import com.dicoding.asclepius.domain.utils.DomainConstants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Long.timestampToDateString(): String {
    val date = Date(this)
    val calendar = Calendar.getInstance()
    calendar.time = date

    val df = SimpleDateFormat(DomainConstants.DATE_STRING_FORMAT, Locale.getDefault())
    return df.format(calendar.time)
}

fun String.dateStringToTimestamp(): Long? {
    val df = SimpleDateFormat(DomainConstants.DATE_STRING_FORMAT, Locale.getDefault())
    val date = df.parse(this)
    return date?.time
}