package com.polaralias.letsdoit.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.polaralias.letsdoit.data.db.entities.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE spaceId = :spaceId ORDER BY name")
    fun observeFolders(spaceId: Long): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY name")
    suspend fun listAll(): List<FolderEntity>

    @Upsert
    suspend fun upsert(folder: FolderEntity)
}
