package com.polaralias.letsdoit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.polaralias.letsdoit.data.db.entities.SharedListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedListDao {
    @Query("SELECT * FROM shared_lists ORDER BY createdAt DESC")
    fun observeSharedLists(): Flow<List<SharedListEntity>>

    @Query("SELECT * FROM shared_lists WHERE shareId = :shareId")
    suspend fun getByShareId(shareId: String): SharedListEntity?

    @Query("SELECT * FROM shared_lists WHERE listId = :listId")
    suspend fun getByListId(listId: Long): SharedListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sharedList: SharedListEntity)

    @Update
    suspend fun update(sharedList: SharedListEntity)

    @Query("DELETE FROM shared_lists WHERE shareId = :shareId")
    suspend fun deleteByShareId(shareId: String)

    @Query("DELETE FROM shared_lists")
    suspend fun deleteAll()
}
