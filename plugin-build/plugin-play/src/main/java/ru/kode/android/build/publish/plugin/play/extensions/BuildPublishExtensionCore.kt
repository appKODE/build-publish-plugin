package ru.kode.android.build.publish.plugin.play.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.play.core.PlayDistribution
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishPlayExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val distribution: NamedDomainObjectContainer<PlayDistribution> =
            objectFactory.domainObjectContainer(PlayDistribution::class.java)
    }
