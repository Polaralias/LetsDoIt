package com.polaralias.letsdoit.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface ClickUpService {
    @GET("v2/team")
    suspend fun getTeams(): Response<Unit>

    @GET("v2/list/{listId}/task")
    suspend fun getTasks(@Path("listId") listId: String): Response<Unit>

    @GET("v2/task/{taskId}")
    suspend fun getTask(@Path("taskId") taskId: String): Response<ClickUpTaskDto>

    @PUT("v2/task/{taskId}")
    suspend fun updateTask(
        @Path("taskId") taskId: String,
        @Body payload: ClickUpTaskUpdate,
        @Header("If-Match") etag: String? = null
    ): Response<ClickUpTaskDto>
}

data class ClickUpTaskDto(
    val id: String,
    val name: String,
    val text_content: String?,
    val due_date: Long?,
    val date_updated: Long,
    val status: ClickUpStatusDto?
)

data class ClickUpStatusDto(
    val status: String,
    val type: String? = null
)

data class ClickUpTaskUpdate(
    val name: String,
    val text_content: String?,
    val due_date: Long?
)
