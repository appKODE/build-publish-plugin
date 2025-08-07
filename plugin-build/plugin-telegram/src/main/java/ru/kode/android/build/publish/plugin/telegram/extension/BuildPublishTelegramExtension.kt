package ru.kode.android.build.publish.plugin.telegram.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import ru.kode.android.build.publish.plugin.telegram.config.TelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramChangelogConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramDistributionConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishTelegramExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        val bot: NamedDomainObjectContainer<TelegramBotConfig> =
            objectFactory.domainObjectContainer(TelegramBotConfig::class.java)
        val changelog: NamedDomainObjectContainer<TelegramChangelogConfig> =
            objectFactory.domainObjectContainer(TelegramChangelogConfig::class.java)
        val distribution: NamedDomainObjectContainer<TelegramDistributionConfig> =
            objectFactory.domainObjectContainer(TelegramDistributionConfig::class.java)

        fun botDefault(configurationAction: Action<TelegramBotConfig>) {
            prepareDefault(bot, configurationAction)
        }

        fun changelogDefault(configurationAction: Action<TelegramChangelogConfig>) {
            prepareDefault(changelog, configurationAction)
        }

        fun distributionDefault(configurationAction: Action<TelegramDistributionConfig>) {
            prepareDefault(distribution, configurationAction)
        }
    }
