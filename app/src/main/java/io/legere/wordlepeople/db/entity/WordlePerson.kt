@file:Suppress("unused")

package io.legere.wordlepeople.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(
    tableName = "wordle_people",
    indices = [
        Index(value = ["gender"], unique = false),
        Index(value = ["color"], unique = false),
    ]
)
data class WordlePerson(
    @PrimaryKey(autoGenerate = true)
    override var id: Long? = null,
    var firstName: String = "",
    var middleName: String = "",
    var lastName: String = "",
    @TypeConverters(Gender::class)
    var gender: Gender,
    @TypeConverters(Color::class)
    var color: Color
): BaseEntity()
