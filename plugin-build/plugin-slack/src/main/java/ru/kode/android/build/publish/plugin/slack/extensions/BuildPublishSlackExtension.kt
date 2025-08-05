package ru.kode.android.build.publish.plugin.slack.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import ru.kode.android.build.publish.plugin.slack.core.SlackBotConfig
import ru.kode.android.build.publish.plugin.slack.core.SlackChangelogConfig
import ru.kode.android.build.publish.plugin.slack.core.SlackDistributionConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishSlackExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        val bot: NamedDomainObjectContainer<SlackBotConfig> =
            objectFactory.domainObjectContainer(SlackBotConfig::class.java)
        val changelog: NamedDomainObjectContainer<SlackChangelogConfig> =
            objectFactory.domainObjectContainer(SlackChangelogConfig::class.java)
        val distribution: NamedDomainObjectContainer<SlackDistributionConfig> =
            objectFactory.domainObjectContainer(SlackDistributionConfig::class.java)

        fun botDefault(configurationAction: Action<SlackBotConfig>) {
            prepareDefault(bot, configurationAction)
        }

        fun changelogDefault(configurationAction: Action<SlackChangelogConfig>) {
            prepareDefault(changelog, configurationAction)
        }

        fun distributionDefault(configurationAction: Action<SlackDistributionConfig>) {
            prepareDefault(distribution, configurationAction)
        }
    }
