package ru.kode.android.build.publish.plugin.firebase.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.firebase.config.FirebaseDistributionConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishFirebaseExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        val distribution: NamedDomainObjectContainer<FirebaseDistributionConfig> =
            objectFactory.domainObjectContainer(FirebaseDistributionConfig::class.java)

        val distributionConfig: (buildName: String) -> FirebaseDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        val distributionConfigOrNull: (buildName: String) -> FirebaseDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        fun distribution(configurationAction: Action<BaseDomainContainer<FirebaseDistributionConfig>>) {
            val container = BaseDomainContainer(distribution)
            configurationAction.execute(container)
        }

        fun distributionCommon(configurationAction: Action<FirebaseDistributionConfig>) {
            common(distribution, configurationAction)
        }
    }
