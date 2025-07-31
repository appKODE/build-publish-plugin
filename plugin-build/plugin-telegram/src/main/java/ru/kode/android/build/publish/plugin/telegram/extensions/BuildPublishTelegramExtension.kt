package ru.kode.android.build.publish.plugin.telegram.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.telegram.core.TelegramConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishTelegramExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val telegram: NamedDomainObjectContainer<TelegramConfig> =
            objectFactory.domainObjectContainer(TelegramConfig::class.java)
    }
