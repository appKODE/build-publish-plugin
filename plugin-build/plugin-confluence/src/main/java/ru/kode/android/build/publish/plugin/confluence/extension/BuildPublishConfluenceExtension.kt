package ru.kode.android.build.publish.plugin.confluence.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.confluence.config.ConfluenceAuthConfig
import ru.kode.android.build.publish.plugin.confluence.config.ConfluenceDistributionConfig
import ru.kode.android.build.publish.plugin.core.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishConfluenceExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        internal val auth: NamedDomainObjectContainer<ConfluenceAuthConfig> =
            objectFactory.domainObjectContainer(ConfluenceAuthConfig::class.java)

        internal val distribution: NamedDomainObjectContainer<ConfluenceDistributionConfig> =
            objectFactory.domainObjectContainer(ConfluenceDistributionConfig::class.java)

        val authConfig: (buildName: String) -> ConfluenceAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        val authConfigOrNull: (buildName: String) -> ConfluenceAuthConfig? = { buildName ->
            auth.getByNameOrNullableCommon(buildName)
        }

        val distributionConfig: (buildName: String) -> ConfluenceDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        val distributionConfigOrNull: (buildName: String) -> ConfluenceDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        fun auth(configurationAction: Action<BaseDomainContainer<ConfluenceAuthConfig>>) {
            val container = BaseDomainContainer(auth)
            configurationAction.execute(container)
        }

        fun distribution(configurationAction: Action<BaseDomainContainer<ConfluenceDistributionConfig>>) {
            val container = BaseDomainContainer(distribution)
            configurationAction.execute(container)
        }

        fun authCommon(configurationAction: Action<ConfluenceAuthConfig>) {
            common(auth, configurationAction)
        }

        fun distributionCommon(configurationAction: Action<ConfluenceDistributionConfig>) {
            common(distribution, configurationAction)
        }
    }
