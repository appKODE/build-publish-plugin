package ru.kode.android.build.publish.plugin.confluence.config

import org.gradle.api.provider.Property

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

    /**
     * Confluence baseUrl
     */
    val baseUrl: Property<String>
}
