package ru.kode.android.build.publish.plugin.base.extension

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.base.extension.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.base.extension.config.OutputConfig
import javax.inject.Inject

const val BASE_EXTENSION_NAME = "buildPublishBase"

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishBaseExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val output: NamedDomainObjectContainer<OutputConfig> =
            objectFactory.domainObjectContainer(OutputConfig::class.java)

        val changelog: NamedDomainObjectContainer<ChangelogConfig> =
            objectFactory.domainObjectContainer(ChangelogConfig::class.java)
    }
