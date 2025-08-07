package ru.kode.android.build.publish.plugin.play.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import ru.kode.android.build.publish.plugin.play.config.PlayAuth
import ru.kode.android.build.publish.plugin.play.config.PlayDistribution
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishPlayExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        val auth: NamedDomainObjectContainer<PlayAuth> =
            objectFactory.domainObjectContainer(PlayAuth::class.java)
        val distribution: NamedDomainObjectContainer<PlayDistribution> =
            objectFactory.domainObjectContainer(PlayDistribution::class.java)

        fun authDefault(configurationAction: Action<PlayAuth>) {
            prepareDefault(auth, configurationAction)
        }

        fun distributionDefault(configurationAction: Action<PlayDistribution>) {
            prepareDefault(distribution, configurationAction)
        }
    }
