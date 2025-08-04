package ru.kode.android.build.publish.plugin.telegram.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.telegram.core.TelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.core.TelegramChangelogConfig
import ru.kode.android.build.publish.plugin.telegram.core.TelegramDistributionConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishTelegramExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val bot: NamedDomainObjectContainer<TelegramBotConfig> =
            objectFactory.domainObjectContainer(TelegramBotConfig::class.java)
        val changelog: NamedDomainObjectContainer<TelegramChangelogConfig> =
            objectFactory.domainObjectContainer(TelegramChangelogConfig::class.java)
        val distribution: NamedDomainObjectContainer<TelegramDistributionConfig> =
            objectFactory.domainObjectContainer(TelegramDistributionConfig::class.java)
    }
