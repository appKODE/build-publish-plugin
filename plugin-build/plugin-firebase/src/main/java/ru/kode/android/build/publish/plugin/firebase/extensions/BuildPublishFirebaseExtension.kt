package ru.kode.android.build.publish.plugin.firebase.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import ru.kode.android.build.publish.plugin.firebase.core.FirebaseDistributionConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishFirebaseExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        val distribution: NamedDomainObjectContainer<FirebaseDistributionConfig> =
            objectFactory.domainObjectContainer(FirebaseDistributionConfig::class.java)

        fun distributionDefault(configurationAction: Action<FirebaseDistributionConfig>) {
            prepareDefault(distribution, configurationAction)
        }
    }
