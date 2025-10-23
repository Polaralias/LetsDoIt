package com.letsdoit.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.letsdoit.app.data.db.entities.SpaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {
    @Query("SELECT * FROM spaces ORDER BY name")
    fun observeSpaces(): Flow<List<SpaceEntity>>

    @Query("SELECT * FROM spaces WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): SpaceEntity?

    @Upsert
    suspend fun upsert(space: SpaceEntity)
}
