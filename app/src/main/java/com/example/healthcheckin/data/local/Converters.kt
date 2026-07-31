package com.example.healthcheckin.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun instantToLong(instant: Instant?): Long? = instant?.toEpochMilli()
}
