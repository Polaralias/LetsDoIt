package com.letsdoit.app.data.repository

import com.letsdoit.app.data.local.dao.FolderDao
import com.letsdoit.app.data.local.dao.ListDao
import com.letsdoit.app.data.local.dao.SpaceDao
import com.letsdoit.app.data.local.entity.FolderEntity
import com.letsdoit.app.data.local.entity.ListEntity
import com.letsdoit.app.data.local.entity.SpaceEntity
import com.letsdoit.app.data.remote.ClickUpApi
import com.letsdoit.app.data.remote.dto.ClickUpFolderDto
import com.letsdoit.app.data.remote.dto.ClickUpListDto
import com.letsdoit.app.data.remote.dto.ClickUpSpaceDto
import com.letsdoit.app.domain.model.Project
import com.letsdoit.app.domain.model.ProjectType
import com.letsdoit.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val api: ClickUpApi,
    private val spaceDao: SpaceDao,
    private val folderDao: FolderDao,
    private val listDao: ListDao
) : ProjectRepository {

    override fun getProjects(): Flow<List<Project>> {
        return combine(
            spaceDao.getAllSpaces(),
            folderDao.getAllFolders(),
            listDao.getAllLists()
        ) { spaces, folders, lists ->
            val projects = mutableListOf<Project>()

            spaces.forEach { space ->
                projects.add(Project(space.id, space.name, null, ProjectType.SPACE, null))
            }

            folders.forEach { folder ->
                projects.add(Project(folder.id, folder.name, null, ProjectType.FOLDER, folder.spaceId))
            }

            lists.forEach { list ->
                val parentId = list.folderId ?: list.spaceId
                projects.add(Project(list.id, list.name, list.color, ProjectType.LIST, parentId))
            }

            projects
        }
    }

    override suspend fun syncStructure() {
        try {
            val teamsResponse = api.getTeams()
            if (teamsResponse.teams.isEmpty()) return

            // Syncing first team's spaces for now
            val teamId = teamsResponse.teams[0].id

            val spacesResponse = api.getSpaces(teamId)
            spacesResponse.spaces.forEach { spaceDto ->
                spaceDao.insertSpace(spaceDto.toEntity())

                try {
                    val foldersResponse = api.getFolders(spaceDto.id)
                    foldersResponse.folders.forEach { folderDto ->
                        folderDao.insertFolder(folderDto.toEntity(spaceDto.id))

                        try {
                            val listsResponse = api.getListsInFolder(folderDto.id)
                            listsResponse.lists.forEach { listDto ->
                                listDao.insertList(listDto.toEntity(spaceDto.id, folderDto.id))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    val listsResponse = api.getListsInSpace(spaceDto.id)
                    listsResponse.lists.forEach { listDto ->
                        listDao.insertList(listDto.toEntity(spaceDto.id, null))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ClickUpSpaceDto.toEntity(): SpaceEntity {
        return SpaceEntity(
            id = id,
            name = name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun ClickUpFolderDto.toEntity(spaceId: String): FolderEntity {
        return FolderEntity(
            id = id,
            spaceId = spaceId,
            name = name,
            orderIndex = orderindex ?: 0
        )
    }

    private fun ClickUpListDto.toEntity(spaceId: String, folderId: String?): ListEntity {
        return ListEntity(
            id = id,
            folderId = folderId,
            spaceId = spaceId,
            name = name,
            color = ""
        )
    }
}
