package ru.kode.android.build.publish.plugin.play.core

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface PlayDistribution {
    val name: String

    /**
     * Track name of target app. Defaults to "internal"
     */
    @get:Input
    val trackId: Property<String>

    /**
     * Test groups for app distribution
     *
     * For example: [android-testers]
     */
    @get:Input
    val updatePriority: Property<Int>
}
