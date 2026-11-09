package ru.kode.android.build.publish.plugin.yandexdisk.controller

import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import ru.kode.android.build.publish.plugin.yandexdisk.controller.entity.YandexDiskShare
import ru.kode.android.build.publish.plugin.yandexdisk.messages.blankRemotePathMessage
import ru.kode.android.build.publish.plugin.yandexdisk.messages.publicUrlMissingMessage
import ru.kode.android.build.publish.plugin.yandexdisk.messages.requestFailedMessage
import ru.kode.android.build.publish.plugin.yandexdisk.network.api.YandexDiskApi
import java.io.File
import java.io.IOException

private const val DISK_PREFIX = "disk:/"
private const val CONFLICT_CODE = 409

internal class YandexDiskControllerImpl(
    private val api: YandexDiskApi,
) : YandexDiskController {
    override fun uploadFile(
        remotePath: String,
        remoteFileName: String,
        file: File,
    ) {
        val normalizedRemotePath = normalizeRemotePath(remotePath)
        ensureFolders(normalizedRemotePath)

        // Step 1: ask the REST API for a one-time upload URL (valid for 30 minutes).
        val uploadLink =
            api
                .getUploadLink(path = toDiskPath(resolveRemoteFilePath(normalizedRemotePath, remoteFileName)))
                .executeBody()
        // Step 2: PUT the bytes to that URL. 201 Created and 202 Accepted are both success.
        api.uploadByHref(href = uploadLink.href, body = file.asRequestBody()).executeUnit()
    }

    override fun publishFile(
        remotePath: String,
        remoteFileName: String,
    ): YandexDiskShare {
        val diskPath = toDiskPath(resolveRemoteFilePath(remotePath, remoteFileName))

        getPublicUrl(diskPath)?.let { return YandexDiskShare(url = it, reused = true) }

        api.publishResource(path = diskPath).executeUnit()

        val publicUrl = getPublicUrl(diskPath) ?: throw IOException(publicUrlMissingMessage(diskPath))
        return YandexDiskShare(url = publicUrl, reused = false)
    }

    override fun resolveRemoteFilePath(
        remotePath: String,
        fileName: String,
    ): String = "${normalizeRemotePath(remotePath)}/${fileName.trim().trim('/')}"

    private fun ensureFolders(remotePath: String) {
        var currentPath = ""
        remotePath.split('/').forEach { part ->
            currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
            // 409 means the folder already exists.
            api.createFolder(path = toDiskPath(currentPath)).executeUnit(extraSuccessCodes = setOf(CONFLICT_CODE))
        }
    }

    private fun getPublicUrl(diskPath: String): String? =
        api
            .getResource(path = diskPath)
            .executeBody()
            .publicUrl
            ?.takeIf { it.isNotBlank() }

    private fun normalizeRemotePath(remotePath: String): String =
        remotePath
            .split('/')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("/")
            .ifEmpty { throw IllegalArgumentException(blankRemotePathMessage()) }

    private fun toDiskPath(path: String): String = "$DISK_PREFIX${path.trim('/')}"
}

private fun <T> Call<T>.executeBody(): T {
    val response = execute()
    val body = response.body()
    if (response.isSuccessful && body != null) return body
    throw IOException(
        requestFailedMessage(request().url.encodedPath, response.code(), response.errorBody()?.string().orEmpty()),
    )
}

private fun Call<Unit>.executeUnit(extraSuccessCodes: Set<Int> = emptySet()) {
    val response = execute()
    val code = response.code()
    val errorBody = response.errorBody()?.string().orEmpty()
    if (response.isSuccessful || code in extraSuccessCodes) return
    throw IOException(requestFailedMessage(request().url.encodedPath, code, errorBody))
}
