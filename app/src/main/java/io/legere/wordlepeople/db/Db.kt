package io.legere.wordlepeople.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.legere.wordlepeople.db.converters.ColorConverter
import io.legere.wordlepeople.db.converters.GenderConverter
import io.legere.wordlepeople.db.dao.WordlePersonDao
import io.legere.wordlepeople.db.entity.WordlePerson


@Database(
    version = 2,
    entities = [
        WordlePerson::class,
    ],
    exportSchema = true
)
@TypeConverters(
    GenderConverter::class,
    ColorConverter::class,
)
abstract class WorldePeopleDb : RoomDatabase() {
    abstract fun wordlePersonDao(): WordlePersonDao

    companion object {
        @Volatile private var instance: WorldePeopleDb? = null

        fun getInstance(context: Context): WorldePeopleDb {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context) : WorldePeopleDb {
            return Room.databaseBuilder(
                context,
                WorldePeopleDb::class.java, "database-name"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
    }
}
