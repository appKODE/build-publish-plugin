package ru.kode.android.build.publish.plugin.confluence.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.confluence.config.ConfluenceAuthConfig
import ru.kode.android.build.publish.plugin.confluence.config.ConfluenceDistributionConfig
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishConfluenceExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        val auth: NamedDomainObjectContainer<ConfluenceAuthConfig> =
            objectFactory.domainObjectContainer(ConfluenceAuthConfig::class.java)
        val distribution: NamedDomainObjectContainer<ConfluenceDistributionConfig> =
            objectFactory.domainObjectContainer(ConfluenceDistributionConfig::class.java)

        fun authDefault(configurationAction: Action<ConfluenceAuthConfig>) {
            prepareDefault(auth, configurationAction)
        }

        fun distributionDefault(configurationAction: Action<ConfluenceDistributionConfig>) {
            prepareDefault(distribution, configurationAction)
        }
    }
