package ru.kode.android.build.publish.plugin.nextcloud.controller

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudShareMode
import ru.kode.android.build.publish.plugin.nextcloud.controller.entity.NextcloudShare
import ru.kode.android.build.publish.plugin.nextcloud.controller.entity.NextcloudSharingResult
import ru.kode.android.build.publish.plugin.nextcloud.messages.cannotCreateShareMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.cannotCreateTypedShareMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.cannotGetSharesMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.cannotResolveInternalLinkFileIdMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.noInternalRecipientsConfiguredMessage
import ru.kode.android.build.publish.plugin.nextcloud.network.api.NextcloudApi
import ru.kode.android.build.publish.plugin.nextcloud.network.entity.OcsJsonResponse
import ru.kode.android.build.publish.plugin.nextcloud.network.entity.OcsMeta
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val OCS_SUCCESS_CODE = 100
private const val OCS_OK_STATUS_CODE = 200
private const val USER_SHARE_TYPE = 0
private const val GROUP_SHARE_TYPE = 1
private const val PUBLIC_SHARE_TYPE = 3
private const val READ_ONLY_PERMISSION = 1

internal class NextcloudControllerImpl(
    baseUrl: String,
    private val username: String,
    private val api: NextcloudApi,
) : NextcloudController {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override fun uploadFile(
        remotePath: String,
        remoteFileName: String,
        file: File,
    ) {
        val normalizedRemotePath = normalizeRemotePath(remotePath)
        val remoteFilePath = resolveRemoteFilePath(normalizedRemotePath, remoteFileName)
        val encodedRemoteFilePath = encodePathSegments(remoteFilePath)
        val davUser = resolveDavUser()

        runCatching {
            uploadFileByDav(
                davUser = davUser,
                encodedRemoteFilePath = encodedRemoteFilePath,
                file = file,
            )
        }.onFailure { throwable ->
            val error = throwable as? NextcloudHttpException
            if (error == null || (error.code != 404 && error.code != 409)) {
                throw throwable
            }
            ensureFolders(
                davUser = davUser,
                remotePath = normalizedRemotePath,
            )
            uploadFileByDav(
                davUser = davUser,
                encodedRemoteFilePath = encodedRemoteFilePath,
                file = file,
            )
        }
    }

    override fun shareFile(
        remotePath: String,
        remoteFileName: String,
        shareMode: NextcloudShareMode,
        userRecipients: Set<String>,
        groupRecipients: Set<String>,
    ): NextcloudSharingResult {
        val normalizedPath = normalizeRemotePath(remotePath)
        val sharePath = "/${resolveRemoteFilePath(normalizedPath, remoteFileName)}"

        return when (shareMode) {
            NextcloudShareMode.PUBLIC_LINK -> {
                val publicShare = createOrReusePublicShare(sharePath)
                NextcloudSharingResult(
                    mode = NextcloudShareMode.PUBLIC_LINK,
                    linkUrl = publicShare.url,
                    createdCount = if (publicShare.reused) 0 else 1,
                    reusedCount = if (publicShare.reused) 1 else 0,
                )
            }

            NextcloudShareMode.INTERNAL_RECIPIENTS -> {
                createOrReuseInternalShares(
                    path = sharePath,
                    userRecipients = userRecipients,
                    groupRecipients = groupRecipients,
                )
            }
        }
    }

    override fun resolveRemoteFilePath(
        remotePath: String,
        fileName: String,
    ): String {
        val normalizedPath = normalizeRemotePath(remotePath)
        val normalizedFileName = normalizeRemoteFileName(fileName)
        return "$normalizedPath/$normalizedFileName"
    }

    private fun uploadFileByDav(
        davUser: String,
        encodedRemoteFilePath: String,
        file: File,
    ) {
        api.uploadFile(
            username = davUser,
            remoteFilePath = encodedRemoteFilePath,
            body = file.asRequestBody(),
        ).executeNoResult(setOf(200, 201, 204))
    }

    private fun ensureFolders(
        davUser: String,
        remotePath: String,
    ) {
        val pathParts = remotePath.split('/').filter { it.isNotBlank() }
        var currentPath = ""
        pathParts.forEach { part ->
            currentPath = if (currentPath.isBlank()) part else "$currentPath/$part"
            api.createFolder(
                username = davUser,
                remoteFolderPath = encodePathSegments(currentPath),
            ).executeNoResult(successCodes = setOf(200, 201, 204, 301, 405))
        }
    }

    private fun resolveDavUser(): String {
        val response = runCatching { api.getCurrentUser().executeOcs() }.getOrNull() ?: return username
        if (!response.ocs.meta.isSuccess()) return username

        val davUser =
            (response.ocs.data as? JsonObject)
                ?.get("id")
                ?.jsonPrimitive
                ?.contentOrNull
                ?.trim()

        return davUser?.takeIf { it.isNotBlank() } ?: username
    }

    private fun createOrReusePublicShare(path: String): NextcloudShare {
        findShare(path = path, shareType = PUBLIC_SHARE_TYPE, shareWith = null)?.let { existingShare ->
            if (!existingShare.url.isNullOrBlank()) {
                return NextcloudShare(
                    url = existingShare.url,
                    reused = true,
                    fileId = existingShare.fileId,
                    shareType = PUBLIC_SHARE_TYPE,
                )
            }
        }

        val createResponse =
            api.createShare(
                path = path,
                shareType = PUBLIC_SHARE_TYPE,
                permissions = READ_ONLY_PERMISSION,
            ).executeOcs()

        if (createResponse.ocs.meta.isSuccess()) {
            parseShares(createResponse.ocs.data).firstOrNull()?.let { createdShare ->
                if (!createdShare.url.isNullOrBlank()) {
                    return NextcloudShare(
                        url = createdShare.url,
                        reused = false,
                        fileId = createdShare.fileId,
                        shareType = PUBLIC_SHARE_TYPE,
                    )
                }
            }
        }

        findShare(path = path, shareType = PUBLIC_SHARE_TYPE, shareWith = null)?.let { existingShare ->
            if (!existingShare.url.isNullOrBlank()) {
                return NextcloudShare(
                    url = existingShare.url,
                    reused = true,
                    fileId = existingShare.fileId,
                    shareType = PUBLIC_SHARE_TYPE,
                )
            }
        }

        throw IllegalStateException(
            cannotCreateShareMessage(
                path = path,
                statusCode = createResponse.ocs.meta.statusCode,
                statusMessage = createResponse.ocs.meta.message.orEmpty(),
            ),
        )
    }

    private fun createOrReuseInternalShares(
        path: String,
        userRecipients: Set<String>,
        groupRecipients: Set<String>,
    ): NextcloudSharingResult {
        val normalizedUsers = normalizeRecipients(userRecipients)
        val normalizedGroups = normalizeRecipients(groupRecipients)

        if (normalizedUsers.isEmpty() && normalizedGroups.isEmpty()) {
            throw IllegalStateException(noInternalRecipientsConfiguredMessage())
        }

        val shares = getShares(path).toMutableList()
        val recipients =
            normalizedUsers.map { USER_SHARE_TYPE to it } +
                normalizedGroups.map { GROUP_SHARE_TYPE to it }

        var createdCount = 0
        var reusedCount = 0

        recipients.forEach { (shareType, recipient) ->
            val existingShare =
                shares.firstOrNull { share ->
                    share.shareType == shareType && share.shareWith == recipient
                }

            if (existingShare != null) {
                reusedCount += 1
                return@forEach
            }

            val createdShare =
                createTypedShare(
                    path = path,
                    shareType = shareType,
                    shareWith = recipient,
                )
            shares += createdShare
            createdCount += 1
        }

        val fileId =
            shares.firstNotNullOfOrNull { it.fileId }
                ?: getShares(path).firstNotNullOfOrNull { it.fileId }
                ?: throw IllegalStateException(cannotResolveInternalLinkFileIdMessage(path))

        return NextcloudSharingResult(
            mode = NextcloudShareMode.INTERNAL_RECIPIENTS,
            linkUrl = "$normalizedBaseUrl/f/$fileId",
            createdCount = createdCount,
            reusedCount = reusedCount,
        )
    }

    private fun createTypedShare(
        path: String,
        shareType: Int,
        shareWith: String,
    ): ParsedShare {
        val response =
            api.createShare(
                path = path,
                shareType = shareType,
                shareWith = shareWith,
                permissions = READ_ONLY_PERMISSION,
            ).executeOcs()

        if (!response.ocs.meta.isSuccess()) {
            throw IllegalStateException(
                cannotCreateTypedShareMessage(
                    path = path,
                    shareType = shareType,
                    shareWith = shareWith,
                    statusCode = response.ocs.meta.statusCode,
                    statusMessage = response.ocs.meta.message.orEmpty(),
                ),
            )
        }

        return parseShares(response.ocs.data).firstOrNull()
            ?: ParsedShare(
                shareType = shareType,
                shareWith = shareWith,
                url = null,
                fileId = null,
            )
    }

    private fun findShare(
        path: String,
        shareType: Int,
        shareWith: String?,
    ): ParsedShare? {
        return getShares(path).firstOrNull { share ->
            if (share.shareType != shareType) {
                return@firstOrNull false
            }

            if (shareType == PUBLIC_SHARE_TYPE) {
                true
            } else {
                share.shareWith == shareWith
            }
        }
    }

    private fun getShares(path: String): List<ParsedShare> {
        val sharesResponse = api.getShares(path = path).executeOcs()
        if (!sharesResponse.ocs.meta.isSuccess()) {
            throw IllegalStateException(
                cannotGetSharesMessage(
                    path = path,
                    statusCode = sharesResponse.ocs.meta.statusCode,
                    statusMessage = sharesResponse.ocs.meta.message.orEmpty(),
                ),
            )
        }
        return parseShares(sharesResponse.ocs.data)
    }

    private fun parseShares(data: JsonElement): List<ParsedShare> {
        return when (data) {
            is JsonArray -> data.mapNotNull { (it as? JsonObject)?.toParsedShare() }
            is JsonObject -> listOfNotNull(data.toParsedShare())
            else -> emptyList()
        }
    }

    private fun JsonObject.toParsedShare(): ParsedShare? {
        val shareType = this["share_type"]?.jsonPrimitive?.intOrNull ?: return null
        val shareWith = this["share_with"]?.jsonPrimitive?.contentOrNull
        val url = this["url"]?.jsonPrimitive?.contentOrNull
        val fileId =
            this["item_source"]?.jsonPrimitive?.contentOrNull
                ?: this["file_source"]?.jsonPrimitive?.contentOrNull

        return ParsedShare(
            shareType = shareType,
            shareWith = shareWith,
            url = url,
            fileId = fileId,
        )
    }

    private fun normalizeRemotePath(remotePath: String): String {
        val normalized =
            remotePath
                .trim()
                .trim('/')
                .split('/')
                .filter { it.isNotBlank() }
                .joinToString("/")

        return normalized.ifBlank {
            throw IllegalStateException("remotePath must not be blank")
        }
    }

    private fun normalizeRemoteFileName(remoteFileName: String): String {
        val normalized = remoteFileName.trim().trim('/')
        check(normalized.isNotBlank()) { "remoteFileName must not be blank" }
        check(!normalized.contains('/')) { "remoteFileName must not contain '/'" }
        return normalized
    }

    private fun normalizeRecipients(values: Set<String>): Set<String> {
        return values.map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    private fun encodePathSegments(path: String): String {
        return path
            .split('/')
            .filter { it.isNotBlank() }
            .joinToString("/") { segment ->
                URLEncoder.encode(segment, StandardCharsets.UTF_8)
                    .replace("+", "%20")
            }
    }
}

private data class ParsedShare(
    val shareType: Int,
    val shareWith: String?,
    val url: String?,
    val fileId: String?,
)

private class NextcloudHttpException(
    val code: Int,
    reason: String,
) : IOException("Nextcloud request failed (code=$code): $reason")

private fun Call<Unit>.executeNoResult(
    successCodes: Set<Int>,
    additionalAllowedCodes: Set<Int> = emptySet(),
) {
    val response = execute()
    val code = response.code()
    if (code in successCodes || code in additionalAllowedCodes) {
        response.errorBody()?.close()
        return
    }

    val errorBody = response.errorBody()?.string().orEmpty()
    response.errorBody()?.close()
    throw NextcloudHttpException(code, errorBody)
}

private fun Call<OcsJsonResponse>.executeOcs(): OcsJsonResponse {
    val response = execute()
    if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
            response.errorBody()?.close()
            return body
        }
    }

    val code = response.code()
    val errorBody = response.errorBody()?.string().orEmpty()
    response.errorBody()?.close()
    throw NextcloudHttpException(code, errorBody)
}

private fun OcsMeta.isSuccess(): Boolean {
    return status.equals("ok", ignoreCase = true) ||
        statusCode == OCS_SUCCESS_CODE ||
        statusCode == OCS_OK_STATUS_CODE
}
