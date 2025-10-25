package com.letsdoit.app.backup

import com.letsdoit.app.share.ShareRepository
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

@Singleton
class DefaultDriveBackupClient @Inject constructor(
    private val shareRepository: ShareRepository,
    moshi: Moshi
) : DriveBackupClient {
    private val client = OkHttpClient.Builder().build()
    private val listAdapter = moshi.adapter(DriveFileList::class.java)
    private val fileAdapter = moshi.adapter(DriveFileItem::class.java)
    private val metadataAdapter = moshi.adapter(DriveFileMetadata::class.java)
    private val errorAdapter = moshi.adapter(DriveErrorResponse::class.java)

    override suspend fun listBackups(): List<BackupInfo> {
        val token = requireToken()
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("drive/v3/files")
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name contains 'letsdoit/backups/' and trashed = false")
            .addQueryParameter("orderBy", "createdTime desc")
            .addQueryParameter("fields", "files(id,name,createdTime,size)")
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(request).execute().use { response ->
            val body = requireBodyString(response)
            val list = runCatching { listAdapter.fromJson(body) }.getOrNull()
                ?: throw DriveClientException(response.code, "Invalid response")
            return list.files.mapNotNull { file ->
                val created = runCatching { Instant.parse(file.createdTime) }.getOrNull() ?: return@mapNotNull null
                val size = file.size?.toLongOrNull() ?: 0L
                BackupInfo(id = file.id, name = file.name, createdAt = created, sizeBytes = size)
            }
        }
    }

    override suspend fun download(id: String): ByteArray {
        val token = requireToken()
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("drive/v3/files/$id")
            .addQueryParameter("alt", "media")
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(request).execute().use { response ->
            return requireBodyBytes(response)
        }
    }

    override suspend fun upload(name: String, payload: ByteArray): BackupInfo {
        val token = requireToken()
        val metadata = DriveFileMetadata(name = name, parents = listOf("appDataFolder"), mimeType = "application/octet-stream")
        val metadataJson = metadataAdapter.toJson(metadata)
        val multipart = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(
                Headers.headersOf("Content-Type", "application/json; charset=utf-8"),
                metadataJson.toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .addPart(
                Headers.headersOf("Content-Type", "application/octet-stream"),
                payload.toRequestBody("application/octet-stream".toMediaType())
            )
            .build()
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("upload/drive/v3/files")
            .addQueryParameter("uploadType", "multipart")
            .addQueryParameter("fields", "id,name,createdTime,size")
            .build()
        val request = Request.Builder()
            .url(url)
            .post(multipart)
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(request).execute().use { response ->
            val body = requireBodyString(response)
            val file = runCatching { fileAdapter.fromJson(body) }.getOrNull()
                ?: throw DriveClientException(response.code, "Invalid response")
            val created = runCatching { Instant.parse(file.createdTime) }.getOrNull()
                ?: throw DriveClientException(response.code, "Invalid timestamp")
            val size = file.size?.toLongOrNull() ?: 0L
            return BackupInfo(id = file.id, name = file.name, createdAt = created, sizeBytes = size)
        }
    }

    override suspend fun delete(id: String) {
        val token = requireToken()
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("drive/v3/files/$id")
            .build()
        val request = Request.Builder()
            .url(url)
            .delete()
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 404) {
                throw mapError(response)
            }
        }
    }

    private fun requireToken(): String {
        return shareRepository.driveToken() ?: throw DriveAuthException(401, "Drive token missing")
    }

    private fun requireBodyBytes(response: Response): ByteArray {
        if (!response.isSuccessful) {
            throw mapError(response)
        }
        val body = response.body ?: throw DriveClientException(response.code, "Empty body")
        return body.bytes()
    }

    private fun requireBodyString(response: Response): String {
        return requireBodyBytes(response).toString(Charsets.UTF_8)
    }

    private fun mapError(response: Response): DriveClientException {
        val body = response.body
        val message = try {
            body?.string()?.let { payload ->
                errorAdapter.fromJson(payload)?.error?.message
            }
        } catch (error: IOException) {
            null
        }
        return if (response.code == 401 || response.code == 403) {
            DriveAuthException(response.code, message)
        } else {
            DriveClientException(response.code, message)
        }
    }
}

@JsonClass(generateAdapter = true)
data class DriveFileList(
    val files: List<DriveFileItem>
)

@JsonClass(generateAdapter = true)
data class DriveFileItem(
    val id: String,
    val name: String,
    val createdTime: String,
    val size: String?
)

@JsonClass(generateAdapter = true)
data class DriveFileMetadata(
    val name: String,
    val parents: List<String>,
    val mimeType: String
)

@JsonClass(generateAdapter = true)
data class DriveErrorResponse(
    val error: DriveErrorBody?
)

@JsonClass(generateAdapter = true)
data class DriveErrorBody(
    val message: String?
)
