package ru.kode.android.build.publish.plugin.vk.controller

import ru.kode.android.build.publish.plugin.vk.controller.entity.VkSendResult
import java.io.File

interface VkController {
    /**
     * Uploads [file] as a document and sends it in a message to [peerId]:
     * a user id or `2000000000 + chat_id` for a chat.
     *
     * Works with a community access token that has the `docs` and `messages` rights.
     */
    fun sendDocument(
        peerId: Long,
        file: File,
        title: String,
        message: String?,
    ): VkSendResult
}
