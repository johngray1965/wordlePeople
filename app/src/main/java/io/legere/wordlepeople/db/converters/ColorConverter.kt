@file:Suppress("unused")

package io.legere.wordlepeople.db.converters

import androidx.room.TypeConverter
import io.legere.wordlepeople.db.entity.Color

class ColorConverter {
    @TypeConverter
    fun toColor(type: String?): Color? {
        return if (type == null) null else Color.valueOf(type)
    }

    @TypeConverter
    fun toString(color: Color?): String? {
        return color?.toString()
    }
}
