package ru.kode.android.build.publish.plugin.vk.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import ru.kode.android.gradle.commons.util.CommonConfigMergeable

/**
 * Where and how build artifacts are sent to VK.
 */
interface VkDistributionConfig : CommonConfigMergeable<VkDistributionConfig> {
    val name: String

    /**
     * Conversation that receives the document: a user id or `2000000000 + chat_id` for a chat.
     * A user must have allowed messages from the community.
     */
    @get:Input
    val peerId: Property<Long>

    /**
     * Whether to zip the artifact before uploading. Defaults to `false`.
     * VK may reject some file types, so `true` is the safer choice.
     */
    @get:Input
    @get:Optional
    val compressed: Property<Boolean>

    /**
     * Optional text sent with the document.
     */
    @get:Input
    @get:Optional
    val message: Property<String>

    /**
     * Explicit document title. Defaults to `<baseFileName>-<version>-<variant>.<ext>`.
     */
    @get:Input
    @get:Optional
    val title: Property<String>

    override fun inheritFrom(common: VkDistributionConfig) {
        peerId.convention(common.peerId)
        compressed.convention(common.compressed)
        message.convention(common.message)
        title.convention(common.title)
    }
}
