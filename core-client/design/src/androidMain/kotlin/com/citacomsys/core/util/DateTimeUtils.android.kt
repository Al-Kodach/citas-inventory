package com.citacomsys.core.util

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant
import kotlin.time.toJavaInstant

actual fun Instant.formatted(formatStyle: String): String? {
    val formatStyle = when(formatStyle) {
        "FULL" -> FormatStyle.FULL
        "LONG" -> FormatStyle.LONG
        "MEDIUM" -> FormatStyle.MEDIUM
        "SHORT" -> FormatStyle.SHORT
        else -> return null
    }

    return DateTimeFormatter
        .ofLocalizedDate(formatStyle)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(this.toJavaInstant())
}