package ru.kode.android.build.publish.plugin.slack.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.slack.core.SlackBotConfig
import ru.kode.android.build.publish.plugin.slack.core.SlackChangelogConfig
import ru.kode.android.build.publish.plugin.slack.core.SlackDistributionConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishSlackExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val bot: NamedDomainObjectContainer<SlackBotConfig> =
            objectFactory.domainObjectContainer(SlackBotConfig::class.java)
        val changelog: NamedDomainObjectContainer<SlackChangelogConfig> =
            objectFactory.domainObjectContainer(SlackChangelogConfig::class.java)
        val distribution: NamedDomainObjectContainer<SlackDistributionConfig> =
            objectFactory.domainObjectContainer(SlackDistributionConfig::class.java)
    }
