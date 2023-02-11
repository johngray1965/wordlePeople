package io.legere.wordlepeople.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import io.legere.wordlepeople.db.entity.Color
import io.legere.wordlepeople.db.entity.Gender
import io.legere.wordlepeople.db.entity.WordlePerson

@Dao
abstract class WordlePersonDao : BaseDao<WordlePerson>("wordle_people") {
    @Query("SELECT * FROM wordle_people")
    abstract fun pagingSource(): PagingSource<Int, WordlePerson>

    @Query("SELECT * FROM wordle_people where gender IN (:genderFilter)")
    abstract fun pagingSourceFilterGender(genderFilter: List<Gender>): PagingSource<Int, WordlePerson>

    @Query("SELECT * FROM wordle_people where color IN (:colorFilter)")
    abstract fun pagingSourceFilterColor(colorFilter: List<Color>): PagingSource<Int, WordlePerson>

    @Query("SELECT * FROM wordle_people where color IN (:colorFilter) AND gender IN (:genderFilter)")
    abstract fun pagingSourceFilterGenderAndColor(genderFilter: List<Gender>, colorFilter: List<Color>): PagingSource<Int, WordlePerson>

}
