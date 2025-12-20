package com.polaralias.letsdoit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.polaralias.letsdoit.data.local.entity.ListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ListEntity)

    @Update
    suspend fun updateList(list: ListEntity)

    @Delete
    suspend fun deleteList(list: ListEntity)

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun getListById(id: String): ListEntity?

    @Query("SELECT * FROM lists")
    fun getAllLists(): Flow<List<ListEntity>>
}
