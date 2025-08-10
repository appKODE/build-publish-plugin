package ru.kode.android.build.publish.plugin.appcenter.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.appcenter.config.AppCenterAuthConfig
import ru.kode.android.build.publish.plugin.appcenter.config.AppCenterDistributionConfig
import ru.kode.android.build.publish.plugin.core.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishAppCenterExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        internal val auth: NamedDomainObjectContainer<AppCenterAuthConfig> =
            objectFactory.domainObjectContainer(AppCenterAuthConfig::class.java)

        internal val distribution: NamedDomainObjectContainer<AppCenterDistributionConfig> =
            objectFactory.domainObjectContainer(AppCenterDistributionConfig::class.java)

        fun auth(configurationAction: Action<BaseDomainContainer<AppCenterAuthConfig>>) {
            val container = BaseDomainContainer(auth)
            configurationAction.execute(container)
        }

        fun distribution(configurationAction: Action<BaseDomainContainer<AppCenterDistributionConfig>>) {
            val container = BaseDomainContainer(distribution)
            configurationAction.execute(container)
        }

        fun authCommon(configurationAction: Action<AppCenterAuthConfig>) {
            common(auth, configurationAction)
        }

        fun distributionCommon(configurationAction: Action<AppCenterDistributionConfig>) {
            common(distribution, configurationAction)
        }
    }
