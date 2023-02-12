package io.legere.wordlepeople.ui.main

import android.app.Application
import android.content.res.AssetManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.legere.wordlepeople.db.WorldePeopleDb
import io.legere.wordlepeople.db.entity.Color
import io.legere.wordlepeople.db.entity.Gender
import io.legere.wordlepeople.db.entity.WordlePerson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okio.buffer
import okio.source
import kotlin.random.Random

@Suppress("TooManyFunctions")
class MainViewModel(private val appContext: Application) : AndroidViewModel(appContext) {
    private val stateFlow = MutableStateFlow(FilterState(mutableSetOf(), mutableSetOf()))

    private val wordlePeopleDao = WorldePeopleDb.getInstance(appContext).wordlePersonDao()

    private val colorSet = mutableSetOf<Color>()
    private val genderSet = mutableSetOf<Gender>()

    val colors: Set<Color>
    get() = colorSet.toSet()

    val genders: Set<Gender>
    get() = genderSet.toSet()

    @OptIn(ExperimentalCoroutinesApi::class)
    val wordlePeopleFlow = stateFlow.flatMapLatest {
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                maxSize = PAGE_SIZE * MAX_SIZE_PAGE_SIZE_MULTIPLIER
            ),
            pagingSourceFactory = {
                when {
                    it.colorSet.isEmpty() && it.genderSet.isEmpty() ->
                        wordlePeopleDao.pagingSource()
                    it.colorSet.isNotEmpty() && it.genderSet.isNotEmpty() ->
                        wordlePeopleDao.pagingSourceFilterGenderAndColor(it.genderSet.toList(), it.colorSet.toList())
                    it.colorSet.isNotEmpty() && it.genderSet.isEmpty() ->
                        wordlePeopleDao.pagingSourceFilterColor(it.colorSet.toList())
                    else ->
                        wordlePeopleDao.pagingSourceFilterGender(it.genderSet.toList())
                }

            }
        ).flow
    }

    @OptIn(ExperimentalCoroutinesApi::class)
   val countFlow = stateFlow.flatMapLatest {
        when {
            it.colorSet.isEmpty() && it.genderSet.isEmpty() ->
                wordlePeopleDao.totalCountFlow()
            it.colorSet.isNotEmpty() && it.genderSet.isNotEmpty() ->
                wordlePeopleDao.filterGenderAndColorCount(it.genderSet.toList(), it.colorSet.toList())
            it.colorSet.isNotEmpty() && it.genderSet.isEmpty() ->
                wordlePeopleDao.filterColorCount(it.colorSet.toList())
            else ->
                wordlePeopleDao.filterGenderCount(it.genderSet.toList())
        }
    }.combine(wordlePeopleDao.totalCountFlow()) { currentCount, totalCount ->
        Pair(totalCount, currentCount)
    }

    private fun updateState() {
        viewModelScope.launch(Dispatchers.IO) {
            // Need to make sure the FilterState and its members tha we send to the
            // stateFlow aren't the ones we are modifying.  Otherwise it never thinks
            // there's a chance (its copy and the one we send are one and the same).
            stateFlow.emit(FilterState(colorSet.toMutableSet(), genderSet.toMutableSet()))
        }
    }

    fun clearAll() {
        colorSet.clear()
        genderSet.clear()
        updateState()
    }

    fun clearColor() {
        colorSet.clear()
        updateState()
    }

    private fun adjustColorFilter(checked: Boolean, color: Color) {
        if (checked) {
            colorSet.add(color)
        } else {
            colorSet.remove(color)
        }
        updateState()
    }

    fun setRedFilter(checked: Boolean) {
        adjustColorFilter(checked, Color.Red)
    }

    fun setGreenFilter(checked: Boolean) {
        adjustColorFilter(checked, Color.Green)
    }

    fun setBlueFilter(checked: Boolean) {
        adjustColorFilter(checked, Color.Blue)
    }

    fun clearGender() {
        genderSet.clear()
        updateState()
    }

    private fun adjustGenderFilter(checked: Boolean, gender: Gender) {
        if (checked) {
            genderSet.add(gender)
        } else {
            genderSet.remove(gender)
        }
        updateState()
    }

    fun setNorthFilter(checked: Boolean) {
        adjustGenderFilter(checked, Gender.North)
    }

    fun setSouthFilter(checked: Boolean) {
        adjustGenderFilter(checked, Gender.South)
    }

    fun setEastFilter(checked: Boolean) {
        adjustGenderFilter(checked, Gender.East)
    }

    fun setWestFilter(checked: Boolean) {
        adjustGenderFilter(checked, Gender.West)
    }

    suspend fun loadData(count: Int) = flow {

        emit("Loading words")

        val data = loadWordList()

        emit("Started building the database")
        val insertList = mutableListOf<WordlePerson>()
        val nameSize = data.size
        for (i in 1..count) {
            insertList.add(
                buildRandomWordlePerson(nameSize, data)
            )
            if (insertList.size >= BATCH_SIZE) {
                wordlePeopleDao.insert(
                    insertList
                )
                insertList.clear()
                emit("Imported $i of $count")
            }
        }
        // Add anything left after the last batch
        wordlePeopleDao.insert(
            insertList
        )
        emit("Done")
    }

    private fun buildRandomWordlePerson(
        nameSize: Int,
        data: List<String>
    ): WordlePerson {
        var index = Random.nextInt(0, nameSize)
        val firstName = data[index]
        index = Random.nextInt(0, nameSize)
        val middleName = data[index]
        index = Random.nextInt(0, nameSize)
        val lastName = data[index]
        index = Random.nextInt(0, Gender.values().size)
        val gender = when (index) {
            0 -> Gender.North
            1 -> Gender.South
            2 -> Gender.East
            else -> Gender.West
        }
        index = Random.nextInt(0, Color.values().size)
        val color = when (index) {
            0 -> Color.Blue
            1 -> Color.Green
            else -> Color.Red
        }
        return WordlePerson(
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            gender = gender,
            color = color
        )
    }

    private fun loadWordList(): List<String> {
        val am: AssetManager = appContext.assets
        val data = mutableListOf<String>()

        am.open("wordlist.txt").use { inputStream ->
            inputStream.source().buffer().use { bufferedFileSource ->
                while (true) {
                    val line = bufferedFileSource.readUtf8Line() ?: break
                    data.add(line)
                }

            }
        }
        return data
    }

    fun clearData() {
        viewModelScope.launch {
            wordlePeopleDao.deleteAll()
        }
    }

    fun delete(wordlePerson: WordlePerson) {
        viewModelScope.launch {
            wordlePeopleDao.delete(wordlePerson)
        }
    }

    companion object {
        private const val BATCH_SIZE = 1000
        private const val PAGE_SIZE = 50
        private const val MAX_SIZE_PAGE_SIZE_MULTIPLIER = 5
    }
}

data class FilterState(
    val colorSet: MutableSet<Color>,
    val genderSet: MutableSet<Gender>
)
