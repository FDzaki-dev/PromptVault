package com.elprompter.promptvault.data.db

import androidx.room.TypeConverter
import com.elprompter.promptvault.data.LogLevel

/** Room tidak bisa menyimpan enum secara native -- konversi ke/dari String. */
class Converters {

    @TypeConverter
    fun fromLogLevel(level: LogLevel): String = level.name

    @TypeConverter
    fun toLogLevel(value: String): LogLevel =
        runCatching { LogLevel.valueOf(value) }.getOrDefault(LogLevel.INFO)
}
