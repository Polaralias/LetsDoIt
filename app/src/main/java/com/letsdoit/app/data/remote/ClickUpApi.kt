package com.letsdoit.app.data.remote

import com.letsdoit.app.data.remote.dto.ClickUpCreateTaskRequest
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

    @POST("list/{list_id}/task")
    suspend fun createTask(@Path("list_id") listId: String, @Body task: ClickUpCreateTaskRequest): ClickUpTaskDto

    @PUT("task/{task_id}")
    suspend fun updateTask(@Path("task_id") taskId: String, @Body task: ClickUpUpdateTaskRequest): ClickUpTaskDto
}
