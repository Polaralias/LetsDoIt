package com.example.letsdoit.data

import kotlinx.coroutines.flow.Flow

class ListRepository(private val listDao: ListDao) {
    fun getLists(): Flow<List<ListEntity>> = listDao.getLists()

    fun getList(id: Long): Flow<ListEntity?> = listDao.getList(id)

    suspend fun createList(name: String): Long {
        require(name.isNotBlank())
        val now = System.currentTimeMillis()
        return listDao.insertList(
            ListEntity(
                name = name,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun renameList(id: Long, name: String) {
        require(name.isNotBlank())
        val now = System.currentTimeMillis()
        listDao.renameList(id, name, now)
    }

    suspend fun deleteList(id: Long) {
        listDao.deleteList(id)
    }
}
