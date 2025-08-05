package ru.kode.android.build.publish.plugin.foundation.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import ru.kode.android.build.publish.plugin.foundation.extension.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.foundation.extension.config.OutputConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishBaseExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        val output: NamedDomainObjectContainer<OutputConfig> =
            objectFactory.domainObjectContainer(OutputConfig::class.java)

        val changelog: NamedDomainObjectContainer<ChangelogConfig> =
            objectFactory.domainObjectContainer(ChangelogConfig::class.java)

        fun outputDefault(configurationAction: Action<OutputConfig>) {
            prepareDefault(output, configurationAction)
        }

        fun changelogDefault(configurationAction: Action<ChangelogConfig>) {
            prepareDefault(changelog, configurationAction)
        }
    }
