package com.redcom1988.core.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun String.toLocalDateTime(): LocalDateTime {
    val cleaned = this.replace(Regex("(?<=\\d{2}:\\d{2}:\\d{2})(?:\\.\\d+)?(?:Z|[+-]\\d{2}(:?\\d{2})?)?$"), "")
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val utcDateTime = LocalDateTime.parse(cleaned, formatter)

    return utcDateTime.atOffset(ZoneOffset.UTC)
        .atZoneSameInstant(ZoneId.systemDefault())
        .toLocalDateTime()
}

fun LocalDateTime.toUtcString(): String {
    val utcDateTime = this.atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return utcDateTime.format(formatter)
}
