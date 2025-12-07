package com.letsdoit.app.data.remote

import com.letsdoit.app.data.remote.dto.ClickUpCreateTaskRequest
import com.letsdoit.app.data.remote.dto.ClickUpFolderDto
import com.letsdoit.app.data.remote.dto.ClickUpFoldersResponse
import com.letsdoit.app.data.remote.dto.ClickUpListDto
import com.letsdoit.app.data.remote.dto.ClickUpListsResponse
import com.letsdoit.app.data.remote.dto.ClickUpSpaceDto
import com.letsdoit.app.data.remote.dto.ClickUpSpacesResponse
import com.letsdoit.app.data.remote.dto.ClickUpTaskDto
import com.letsdoit.app.data.remote.dto.ClickUpTasksResponse
import com.letsdoit.app.data.remote.dto.ClickUpUpdateTaskRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ClickUpApi {

    @GET("task/{task_id}")
    suspend fun getTask(@Path("task_id") taskId: String): ClickUpTaskDto

    @GET("list/{list_id}/task")
    suspend fun getTasks(@Path("list_id") listId: String): ClickUpTasksResponse

    @GET("team/{team_id}/space")
    suspend fun getSpaces(@Path("team_id") teamId: String): ClickUpSpacesResponse

    @GET("space/{space_id}/folder")
    suspend fun getFolders(@Path("space_id") spaceId: String): ClickUpFoldersResponse

    @GET("folder/{folder_id}/list")
    suspend fun getListsInFolder(@Path("folder_id") folderId: String): ClickUpListsResponse

    @GET("space/{space_id}/list")
    suspend fun getListsInSpace(@Path("space_id") spaceId: String): ClickUpListsResponse

    @POST("list/{list_id}/task")
    suspend fun createTask(@Path("list_id") listId: String, @Body task: ClickUpCreateTaskRequest): ClickUpTaskDto

    @PUT("task/{task_id}")
    suspend fun updateTask(@Path("task_id") taskId: String, @Body task: ClickUpUpdateTaskRequest): ClickUpTaskDto
}
