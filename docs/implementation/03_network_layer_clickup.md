# Implementation Phase 3: Network Layer (ClickUp)

This document details the implementation of the network layer to communicate with the ClickUp API.

## Goal
Establish a secure and efficient connection to the ClickUp API, enabling data fetching and updates.

## Prompts

### 1. Define API Constants
> Create `core/util/Constants.kt`:
> *   `BASE_URL`: "https://api.clickup.com/api/v2/"
> *   Define keys for SharedPreferences (e.g., "access_token").

### 2. Create DTOs
> Create data classes in `data/remote/dto/` matching ClickUp API JSON responses:
> *   `ClickUpTaskDto` (id, name, text_content, status, due_date, etc.)
> *   `ClickUpListDto`
> *   `ClickUpFolderDto`
> *   `ClickUpSpaceDto`
> *   `ClickUpResponse` wrappers (generic wrappers if the API uses them).

### 3. Define Retrofit Service
> Create `data/remote/ClickUpApi.kt` interface:
> *   `@GET("task/{task_id}") suspend fun getTask(@Path("task_id") taskId: String): ClickUpTaskDto`
> *   `@GET("list/{list_id}/task") suspend fun getTasks(@Path("list_id") listId: String, ...): ClickUpTasksResponse`
> *   `@POST("list/{list_id}/task") suspend fun createTask(...)`
> *   `@PUT("task/{task_id}") suspend fun updateTask(...)`
> *   (Add similar endpoints for Spaces, Folders, and Lists)

### 4. Implement Auth Interceptor
> Create `data/remote/AuthInterceptor.kt`:
> *   Implement `okhttp3.Interceptor`.
> *   Retrieve the API token from `SharedPreferences` (or a generic TokenProvider interface).
> *   Add header `Authorization: <token>` to the request builder.

### 5. Hilt Module for Network
> Create `core/di/NetworkModule.kt`:
> *   Annotate with `@Module` and `@InstallIn(SingletonComponent::class)`.
> *   Provide `OkHttpClient` configured with `AuthInterceptor` and `HttpLoggingInterceptor` (for debug).
> *   Provide `Retrofit` builder using `GsonConverterFactory` (or Moshi).
> *   Provide `ClickUpApi` service.

### 6. Verification
> Create a unit test `test/java/com/letsdoit/app/data/remote/ClickUpApiTest.kt` (using MockWebServer):
> *   Mock a JSON response for `getTask`.
> *   Call the API method.
> *   Verify the DTO is parsed correctly.
