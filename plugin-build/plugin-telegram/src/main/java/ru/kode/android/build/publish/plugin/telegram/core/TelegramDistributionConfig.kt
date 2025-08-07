package ru.kode.android.build.publish.plugin.telegram.core

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface TelegramDistributionConfig {
    val name: String

    /**
     * Should upload build at the same chat or not
     * Works only if file size is smaller then 50 mb
     */
    @get:Input
    @get:Optional
    val uploadBuild: Property<Boolean>
}
