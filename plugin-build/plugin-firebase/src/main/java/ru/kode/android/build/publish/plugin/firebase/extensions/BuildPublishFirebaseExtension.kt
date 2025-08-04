package ru.kode.android.build.publish.plugin.firebase.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.firebase.core.FirebaseDistributionConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishFirebaseExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {

        val distribution: NamedDomainObjectContainer<FirebaseDistributionConfig> =
            objectFactory.domainObjectContainer(FirebaseDistributionConfig::class.java)

    }
