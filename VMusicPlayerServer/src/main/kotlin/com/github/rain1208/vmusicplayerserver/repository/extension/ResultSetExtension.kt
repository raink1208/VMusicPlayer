package com.github.rain1208.vmusicplayerserver.repository.extension

import java.sql.ResultSet
import java.time.Duration
import org.postgresql.util.PGInterval

fun ResultSet.getDuration(columnLabel: String): Duration {
    val interval = this.getObject(columnLabel, PGInterval::class.java)
    val seconds =
        interval.years * 365 * 86400 +
                interval.months * 30 * 86400 +
                interval.days * 86400 +
                interval.hours * 3600 +
                interval.minutes * 60 +
                interval.seconds
    return Duration.ofSeconds(seconds.toLong())
}
