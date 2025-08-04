package ru.kode.android.build.publish.plugin.foundation.extension.config

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
     * Disable logic to obtain versions (version code and version name) from tag,
     * it can be useful for debug builds to not broke gradle cache
     *
     * For details see [explanation](https://youtu.be/7ll-rkLCtyk?si=Qv_LS0weiYCBT0OV&t=943)
     *
     * If disabled, hardcoded values will be used
     *
     * Default: true
     */
    @get:Input
    val useVersionsFromTag: Property<Boolean>

    /**
     * Use default version code and version name values when the useVersionsFromTag is set to false
     *
     * Default: true
     */
    @get:Input
    val useDefaultVersionsAsFallback: Property<Boolean>

    @get:Input
    @get:Optional
    val buildTagPattern: Property<String>
}
