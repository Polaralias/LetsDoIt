package com.letsdoit.app.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ClickUpService {
    @GET("v2/team")
    suspend fun getTeams(): Response<Unit>

    @GET("v2/list/{listId}/task")
    suspend fun getTasks(@Path("listId") listId: String): Response<Unit>
}
