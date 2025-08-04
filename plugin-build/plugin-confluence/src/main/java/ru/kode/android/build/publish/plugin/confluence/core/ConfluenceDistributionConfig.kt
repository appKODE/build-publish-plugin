package ru.kode.android.build.publish.plugin.confluence.core

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface ConfluenceDistributionConfig {
    val name: String

    /**
     * Confluence page id
     */
    @get:Input
    val pageId: Property<String>

}

