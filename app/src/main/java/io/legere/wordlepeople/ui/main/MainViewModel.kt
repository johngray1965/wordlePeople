package io.legere.wordlepeople.ui.main

import android.app.Application
import android.content.res.AssetManager
import android.os.Parcelable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.legere.wordlepeople.db.WorldePeopleDb
import io.legere.wordlepeople.db.entity.Color
import io.legere.wordlepeople.db.entity.Gender
import io.legere.wordlepeople.db.entity.WordlePerson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okio.buffer
import okio.source
import timber.log.Timber
import kotlin.random.Random

@Suppress("TooManyFunctions")
class MainViewModel(
    private val appContext: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(appContext) {

    private val wordlePeopleDao = WorldePeopleDb.getInstance(appContext).wordlePersonDao()

    private val colorSet = mutableSetOf<Color>()
    private val genderSet = mutableSetOf<Gender>()

    val colors: Set<Color>
    get() = colorSet.toSet()

    val genders: Set<Gender>
    get() = genderSet.toSet()

    @OptIn(ExperimentalCoroutinesApi::class)
    val wordlePeopleFlow =
        savedStateHandle.getStateFlow("filter", (FilterState(mutableSetOf(), mutableSetOf())))
            .flatMapLatest {
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                maxSize = PAGE_SIZE * MAX_SIZE_PAGE_SIZE_MULTIPLIER
            ),
            pagingSourceFactory = {
                Timber.d("filter: $it")
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
    val countFlow =
        savedStateHandle.getStateFlow("filter", (FilterState(mutableSetOf(), mutableSetOf())))
            .flatMapLatest {
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

    init {
        if (savedStateHandle.contains("filter")) {
            savedStateHandle.get<FilterState>("filter")?.let {
                Timber.d("init got $it")
                colorSet.clear()
                colorSet.addAll(it.colorSet)
                genderSet.clear()
                genderSet.addAll(it.genderSet)
            }
        }
    }

    private fun updateState() {
        savedStateHandle["filter"] = FilterState(colorSet.toMutableSet(), genderSet.toMutableSet())
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

    fun adjustColorFilter(checked: Boolean, color: Color) {
        if (checked) {
            colorSet.add(color)
        } else {
            colorSet.remove(color)
        }
        updateState()
    }

    fun clearGender() {
        genderSet.clear()
        updateState()
    }

    fun adjustGenderFilter(checked: Boolean, gender: Gender) {
        if (checked) {
            genderSet.add(gender)
        } else {
            genderSet.remove(gender)
        }
        updateState()
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
@Parcelize
data class FilterState(
    val colorSet: MutableSet<Color>,
    val genderSet: MutableSet<Gender>
): Parcelable
