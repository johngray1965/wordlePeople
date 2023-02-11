/*
 * Copyright (c) 2021-2023.  Legere. All rights reserved.
 */

@file:Suppress("unused")

package io.legere.wordlepeople.db.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import io.legere.wordlepeople.db.entity.BaseEntity

@Suppress("TooManyFunctions")
abstract class BaseDao<T : BaseEntity>(private val tableName: String) {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: T): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entities: List<T>): Array<Long>

    @Update
    abstract suspend fun update(entity: T): Int

    @Update
    abstract suspend fun update(entities: List<T>): Int

    @Delete
    abstract suspend fun delete(entity: T): Int

    @Delete
    abstract suspend fun delete(entities: List<T>): Int

    @RawQuery
    protected abstract suspend fun deleteAll(query: SupportSQLiteQuery): Int

    suspend fun deleteAll(): Int {
        val query = SimpleSQLiteQuery("DELETE FROM $tableName")
        return deleteAll(query)
    }

    @RawQuery
    protected abstract suspend fun getEntitySync(query: SupportSQLiteQuery): List<T>?

    suspend fun getEntitySync(id: Long): T? {
        return getEntitySync(listOf(id))?.firstOrNull()
    }

    private suspend fun getEntitySync(ids: List<Long>): List<T>? {
        val result = StringBuilder()
        for (index in ids.indices) {
            if (index != 0) {
                result.append(",")
            }
            result.append("'").append(ids[index]).append("'")
        }
        val query = SimpleSQLiteQuery("SELECT * FROM $tableName WHERE id IN ($result);")
        return getEntitySync(query)
    }

    @RawQuery
    protected abstract suspend fun getAll(query: SupportSQLiteQuery): List<T>

    suspend fun getAll(): List<T> {
        val query = SimpleSQLiteQuery("SELECT * FROM $tableName")
        return getAll(query)
    }
}
