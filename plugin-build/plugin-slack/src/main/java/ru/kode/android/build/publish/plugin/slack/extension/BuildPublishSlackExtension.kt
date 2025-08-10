package ru.kode.android.build.publish.plugin.slack.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BaseExtension
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.slack.config.SlackBotConfig
import ru.kode.android.build.publish.plugin.slack.config.SlackChangelogConfig
import ru.kode.android.build.publish.plugin.slack.config.SlackDistributionConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishSlackExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        internal val bot: NamedDomainObjectContainer<SlackBotConfig> =
            objectFactory.domainObjectContainer(SlackBotConfig::class.java)

        internal val changelog: NamedDomainObjectContainer<SlackChangelogConfig> =
            objectFactory.domainObjectContainer(SlackChangelogConfig::class.java)

        internal val distribution: NamedDomainObjectContainer<SlackDistributionConfig> =
            objectFactory.domainObjectContainer(SlackDistributionConfig::class.java)

        val botConfig: (buildName: String) -> SlackBotConfig = { buildName ->
            bot.getByNameOrRequiredCommon(buildName)
        }

        val botConfigOrNull: (buildName: String) -> SlackBotConfig? = { buildName ->
            bot.getByNameOrNullableCommon(buildName)
        }

        val changelogConfig: (buildName: String) -> SlackChangelogConfig = { buildName ->
            changelog.getByNameOrRequiredCommon(buildName)
        }

        val changelogConfigOrNull: (buildName: String) -> SlackChangelogConfig? = { buildName ->
            changelog.getByNameOrNullableCommon(buildName)
        }

        val distributionConfig: (buildName: String) -> SlackDistributionConfig = { buildName ->
            distribution.getByNameOrRequiredCommon(buildName)
        }

        val distributionConfigOrNull: (buildName: String) -> SlackDistributionConfig? = { buildName ->
            distribution.getByNameOrNullableCommon(buildName)
        }

        fun bot(configurationAction: Action<BaseDomainContainer<SlackBotConfig>>) {
            val container = BaseDomainContainer(bot)
            configurationAction.execute(container)
        }

        fun changelog(configurationAction: Action<BaseDomainContainer<SlackChangelogConfig>>) {
            val container = BaseDomainContainer(changelog)
            configurationAction.execute(container)
        }

        fun distribution(configurationAction: Action<BaseDomainContainer<SlackDistributionConfig>>) {
            val container = BaseDomainContainer(distribution)
            configurationAction.execute(container)
        }

        fun botCommon(configurationAction: Action<SlackBotConfig>) {
            common(bot, configurationAction)
        }

        fun changelogCommon(configurationAction: Action<SlackChangelogConfig>) {
            common(changelog, configurationAction)
        }

        fun distributionCommon(configurationAction: Action<SlackDistributionConfig>) {
            common(distribution, configurationAction)
        }
    }
