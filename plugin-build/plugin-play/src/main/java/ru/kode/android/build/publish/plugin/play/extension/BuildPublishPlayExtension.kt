package ru.kode.android.build.publish.plugin.play.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BaseExtension
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.play.config.PlayAuth
import ru.kode.android.build.publish.plugin.play.config.PlayDistribution
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishPlayExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        internal val auth: NamedDomainObjectContainer<PlayAuth> =
            objectFactory.domainObjectContainer(PlayAuth::class.java)

        internal val distribution: NamedDomainObjectContainer<PlayDistribution> =
            objectFactory.domainObjectContainer(PlayDistribution::class.java)

        val authConfig: (buildName: String) -> PlayAuth = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        val authConfigOrNull: (buildName: String) -> PlayAuth? = { buildName ->
            auth.getByNameOrNullableCommon(buildName)
        }

        val distributionConfig: (buildName: String) -> PlayDistribution = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        val distributionConfigOrNull: (buildName: String) -> PlayDistribution? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        fun auth(configurationAction: Action<BaseDomainContainer<PlayAuth>>) {
            val container = BaseDomainContainer(auth)
            configurationAction.execute(container)
        }

        fun distribution(configurationAction: Action<BaseDomainContainer<PlayDistribution>>) {
            val container = BaseDomainContainer(distribution)
            configurationAction.execute(container)
        }

        fun authCommon(configurationAction: Action<PlayAuth>) {
            common(auth, configurationAction)
        }

        fun distributionCommon(configurationAction: Action<PlayDistribution>) {
            common(distribution, configurationAction)
        }
    }
