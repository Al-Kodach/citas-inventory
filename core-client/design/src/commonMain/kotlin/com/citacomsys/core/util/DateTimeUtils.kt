package com.citacomsys.core.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

fun Instant.formatRelativeTime(now: Instant = Clock.System.now()): String {
    val duration = now - this

    return when {
        duration < 1.minutes -> "Just now"
        duration < 1.hours -> "${duration.inWholeMinutes}m ago"
        duration < 24.hours -> "${duration.inWholeHours}h ago"
        duration < 2.days -> "Yesterday"
        duration < 7.days -> "${duration.inWholeDays}d ago"
        else -> {
            this.toString().substringBefore('T')
        }
    }
}

fun LocalDate.formatted(): String {
    val customFormat = LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        day(padding = Padding.ZERO)
        chars(", ")
        year()
    }

    return this.format(customFormat)
}

fun Instant.toLocalDate(): LocalDate = this.toLocalDateTime(TimeZone.currentSystemDefault()).date

fun LocalDate.toInstant(): Instant = this.atStartOfDayIn(TimeZone.currentSystemDefault())
fun LocalDate.toEpochMilliseconds(): Long = this.toInstant().toEpochMilliseconds()

fun Long.fromEpochMillisecondsToLocalDate(): LocalDate {
    return Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
}

expect fun Instant.formatted(formatStyle: String = "FULL"): String?