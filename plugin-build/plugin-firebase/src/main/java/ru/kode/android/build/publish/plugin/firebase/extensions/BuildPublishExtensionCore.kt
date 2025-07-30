package ru.kode.android.build.publish.plugin.firebase.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.firebase.core.FirebaseAppDistributionConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {

    val firebaseDistribution: NamedDomainObjectContainer<FirebaseAppDistributionConfig> =
        objectFactory.domainObjectContainer(FirebaseAppDistributionConfig::class.java)

    }
