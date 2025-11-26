package com.example.letsdoit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Query("SELECT * FROM lists ORDER BY createdAt ASC")
    fun getLists(): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists WHERE id = :id")
    fun getList(id: Long): Flow<ListEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(listEntity: ListEntity): Long

    @Query("UPDATE lists SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameList(id: Long, name: String, updatedAt: Long)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun deleteList(id: Long)
}
