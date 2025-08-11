package ru.kode.android.build.publish.plugin.telegram.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BaseExtension
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.telegram.config.TelegramBotsConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramChangelogConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramDistributionConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishTelegramExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        internal val bot: NamedDomainObjectContainer<TelegramBotsConfig> =
            objectFactory.domainObjectContainer(TelegramBotsConfig::class.java)

        internal val changelog: NamedDomainObjectContainer<TelegramChangelogConfig> =
            objectFactory.domainObjectContainer(TelegramChangelogConfig::class.java)

        internal val distribution: NamedDomainObjectContainer<TelegramDistributionConfig> =
            objectFactory.domainObjectContainer(TelegramDistributionConfig::class.java)

        val botsConfig: (buildName: String) -> TelegramBotsConfig = { buildName ->
            bot.getByNameOrRequiredCommon(buildName)
        }

        val botsConfigOrNull: (buildName: String) -> TelegramBotsConfig? = { buildName ->
            bot.getByNameOrNullableCommon(buildName)
        }

        val changelogConfig: (buildName: String) -> TelegramChangelogConfig = { buildName ->
            changelog.getByNameOrRequiredCommon(buildName)
        }

        val changelogConfigOrNull: (buildName: String) -> TelegramChangelogConfig? = { buildName ->
            changelog.getByNameOrNullableCommon(buildName)
        }

        val distributionConfig: (buildName: String) -> TelegramDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        val distributionConfigOrNull: (buildName: String) -> TelegramDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        fun bots(configurationAction: Action<BaseDomainContainer<TelegramBotsConfig>>) {
            val container = BaseDomainContainer(bot)
            configurationAction.execute(container)
        }

        fun changelog(configurationAction: Action<BaseDomainContainer<TelegramChangelogConfig>>) {
            val container = BaseDomainContainer(changelog)
            configurationAction.execute(container)
        }

        fun distribution(configurationAction: Action<BaseDomainContainer<TelegramDistributionConfig>>) {
            val container = BaseDomainContainer(distribution)
            configurationAction.execute(container)
        }

        fun botsCommon(configurationAction: Action<TelegramBotsConfig>) {
            common(bot, configurationAction)
        }

        fun changelogCommon(configurationAction: Action<TelegramChangelogConfig>) {
            common(changelog, configurationAction)
        }

        fun distributionCommon(configurationAction: Action<TelegramDistributionConfig>) {
            common(distribution, configurationAction)
        }
    }
