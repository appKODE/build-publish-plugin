package ru.kode.android.build.publish.plugin.core.api.config

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import javax.inject.Inject

abstract class BasicAuthConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : CommonConfigMergeable<BasicAuthConfig> {
        abstract val name: String

        @get:Input
        abstract val baseUrl: Property<String>

        @get:Nested
        val credentials: BasicAuthCredentials =
            objects.newInstance(BasicAuthCredentials::class.java)

        override fun inheritFrom(common: BasicAuthConfig) {
            baseUrl.convention(common.baseUrl)
            credentials.inheritFrom(common.credentials)
        }
    }
