package ru.kode.android.build.publish.plugin.appcenter.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.appcenter.config.AppCenterAuthConfig
import ru.kode.android.build.publish.plugin.appcenter.config.AppCenterDistributionConfig
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishAppCenterExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        val auth: NamedDomainObjectContainer<AppCenterAuthConfig> =
            objectFactory.domainObjectContainer(AppCenterAuthConfig::class.java)
        val distribution: NamedDomainObjectContainer<AppCenterDistributionConfig> =
            objectFactory.domainObjectContainer(AppCenterDistributionConfig::class.java)

        fun authDefault(configurationAction: Action<AppCenterAuthConfig>) {
            prepareDefault(auth, configurationAction)
        }

        fun distributionDefault(configurationAction: Action<AppCenterDistributionConfig>) {
            prepareDefault(distribution, configurationAction)
        }
    }
