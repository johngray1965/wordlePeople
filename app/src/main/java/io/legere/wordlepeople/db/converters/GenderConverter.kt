@file:Suppress("unused")

package io.legere.wordlepeople.db.converters

import androidx.room.TypeConverter
import io.legere.wordlepeople.db.entity.Gender

class GenderConverter {
    @TypeConverter
    fun toGender(type: String?): Gender? {
        return if (type == null) null else Gender.valueOf(type)
    }

    @TypeConverter
    fun toString(gender: Gender?): String? {
        return gender?.toString()
    }
}
