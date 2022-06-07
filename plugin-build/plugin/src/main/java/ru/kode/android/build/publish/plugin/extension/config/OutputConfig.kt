package ru.kode.android.build.publish.plugin.extension.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface OutputConfig {
    val name: String

    /**
     * Application bundle name prefix
     * For example: example-base-project-android
     */
    @get:Input
    val baseFileName: Property<String>

    /**
     * Value from version code should start (optional)
     */
    @get:Input
    @get:Optional
    val startVersionCode: Property<Int>
}
