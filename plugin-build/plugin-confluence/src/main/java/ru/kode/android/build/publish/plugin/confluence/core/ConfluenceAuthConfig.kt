package ru.kode.android.build.publish.plugin.confluence.core

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface ConfluenceAuthConfig {
    val name: String

    /**
     * Confluence user name
     */
    val username: Property<String>

    /**
     * Confluence user password
     */
    val password: Property<String>

}

