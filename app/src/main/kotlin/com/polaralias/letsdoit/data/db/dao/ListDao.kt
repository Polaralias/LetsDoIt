package com.polaralias.letsdoit.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.polaralias.letsdoit.data.db.entities.ListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Query("SELECT * FROM lists WHERE spaceId = :spaceId ORDER BY name")
    fun observeListsInSpace(spaceId: Long): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists WHERE folderId = :folderId ORDER BY name")
    fun observeListsInFolder(folderId: Long): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists ORDER BY name")
    fun observeAll(): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun findById(id: Long): ListEntity?

    @Query("SELECT * FROM lists WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): ListEntity?

    @Query("SELECT * FROM lists ORDER BY name")
    suspend fun listAll(): List<ListEntity>

    @Upsert
    suspend fun upsert(list: ListEntity)
}
