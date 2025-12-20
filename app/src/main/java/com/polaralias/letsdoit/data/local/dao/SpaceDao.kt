package com.polaralias.letsdoit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.polaralias.letsdoit.data.local.entity.SpaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: SpaceEntity)

    @Update
    suspend fun updateSpace(space: SpaceEntity)

    @Delete
    suspend fun deleteSpace(space: SpaceEntity)

    @Query("SELECT * FROM spaces WHERE id = :id")
    suspend fun getSpaceById(id: String): SpaceEntity?

    @Query("SELECT * FROM spaces")
    fun getAllSpaces(): Flow<List<SpaceEntity>>
}
