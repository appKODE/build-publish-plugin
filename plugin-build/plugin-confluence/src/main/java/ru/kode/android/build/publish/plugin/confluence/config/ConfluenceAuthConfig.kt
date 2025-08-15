package ru.kode.android.build.publish.plugin.confluence.config

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import javax.inject.Inject

abstract class ConfluenceAuthConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        abstract val name: String

        /**
         * Confluence baseUrl
         */
        abstract val baseUrl: Property<String>

        @get:Nested
        val credentials: BasicAuthCredentials =
            objects.newInstance(BasicAuthCredentials::class.java)
    }
