package ru.kode.android.build.publish.plugin.appcenter.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.appcenter.core.AppCenterAuthConfig
import ru.kode.android.build.publish.plugin.appcenter.core.AppCenterDistributionConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishAppCenterExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val auth: NamedDomainObjectContainer<AppCenterAuthConfig> =
            objectFactory.domainObjectContainer(AppCenterAuthConfig::class.java)
        val distribution: NamedDomainObjectContainer<AppCenterDistributionConfig> =
            objectFactory.domainObjectContainer(AppCenterDistributionConfig::class.java)
    }
