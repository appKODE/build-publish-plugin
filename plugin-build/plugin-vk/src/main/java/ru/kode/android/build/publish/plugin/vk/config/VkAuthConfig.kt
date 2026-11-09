package ru.kode.android.build.publish.plugin.vk.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import ru.kode.android.gradle.commons.util.CommonConfigMergeable

/**
 * VK authentication for a build variant (or the common configuration).
 */
interface VkAuthConfig : CommonConfigMergeable<VkAuthConfig> {
    val name: String

    /**
     * File containing a VK community access token with the `docs` and `messages` rights
     * (community settings, "API usage", "Access tokens").
     */
    @get:InputFile
    val accessTokenFile: RegularFileProperty

    override fun inheritFrom(common: VkAuthConfig) {
        accessTokenFile.convention(common.accessTokenFile)
    }
}
