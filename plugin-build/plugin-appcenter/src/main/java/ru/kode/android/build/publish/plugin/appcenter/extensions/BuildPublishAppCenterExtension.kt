package ru.kode.android.build.publish.plugin.appcenter.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.appcenter.core.AppCenterDistributionConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishAppCenterExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val appCenterDistribution: NamedDomainObjectContainer<AppCenterDistributionConfig> =
            objectFactory.domainObjectContainer(AppCenterDistributionConfig::class.java)
    }
