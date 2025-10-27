package com.polaralias.letsdoit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.polaralias.letsdoit.data.db.entities.SpaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {
    @Query("SELECT * FROM spaces ORDER BY name")
    fun observeSpaces(): Flow<List<SpaceEntity>>

    @Query("SELECT * FROM spaces WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): SpaceEntity?

    @Query("SELECT * FROM spaces ORDER BY name")
    suspend fun listAll(): List<SpaceEntity>

    @Upsert
    suspend fun upsert(space: SpaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spaces: List<SpaceEntity>)

    @Query("DELETE FROM spaces")
    suspend fun clear()
}
