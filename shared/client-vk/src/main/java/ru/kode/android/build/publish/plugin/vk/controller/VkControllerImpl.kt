package ru.kode.android.build.publish.plugin.vk.controller

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import ru.kode.android.build.publish.plugin.vk.controller.entity.VkSendResult
import ru.kode.android.build.publish.plugin.vk.messages.httpFailedMessage
import ru.kode.android.build.publish.plugin.vk.messages.methodFailedMessage
import ru.kode.android.build.publish.plugin.vk.messages.uploadFailedMessage
import ru.kode.android.build.publish.plugin.vk.network.api.VkApi
import ru.kode.android.build.publish.plugin.vk.network.entity.VkResponse
import java.io.File
import java.io.IOException
import kotlin.random.Random

internal class VkControllerImpl(
    private val api: VkApi,
) : VkController {
    override fun sendDocument(
        peerId: Long,
        file: File,
        title: String,
        message: String?,
    ): VkSendResult {
        val uploadUrl =
            api
                .getMessagesUploadServer(
                    peerId = peerId,
                ).executeMethod("docs.getMessagesUploadServer")
                .uploadUrl

        val part =
            MultipartBody.Part.createFormData(
                name = "file",
                filename = title,
                body = file.asRequestBody("application/octet-stream".toMediaType()),
            )
        val upload = api.uploadFile(url = uploadUrl, file = part).executeHttp()
        val uploadedFile =
            upload.file?.takeIf { it.isNotBlank() }
                ?: throw IOException(uploadFailedMessage(upload.error, upload.errorDescription))

        val doc = api.saveDocument(file = uploadedFile, title = title).executeMethod("docs.save").doc
        val attachment = listOfNotNull("doc${doc.ownerId}_${doc.id}", doc.accessKey).joinToString("_")

        val messageId =
            api
                .sendMessage(
                    peerId = peerId,
                    randomId = Random.nextInt(),
                    message = message?.takeIf { it.isNotBlank() },
                    attachment = attachment,
                ).executeMethod("messages.send")

        return VkSendResult(messageId = messageId, documentUrl = doc.url)
    }
}

private fun <T> Call<T>.executeHttp(): T {
    val response = execute()
    val body = response.body()
    if (response.isSuccessful && body != null) return body
    throw IOException(
        httpFailedMessage(request().url.encodedPath, response.code(), response.errorBody()?.string().orEmpty()),
    )
}

/** VK reports method errors with HTTP 200 and an `error` object instead of `response`. */
private fun <T> Call<VkResponse<T>>.executeMethod(method: String): T {
    val envelope = executeHttp()
    envelope.response?.let { return it }
    throw IOException(methodFailedMessage(method, envelope.error?.code, envelope.error?.message))
}
