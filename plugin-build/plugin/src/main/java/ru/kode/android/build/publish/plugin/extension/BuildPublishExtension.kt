package ru.kode.android.build.publish.plugin.extension

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.appcenter.core.AppCenterDistributionConfig
import ru.kode.android.build.publish.plugin.extension.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.clickup.core.ClickUpConfig
import ru.kode.android.build.publish.plugin.confluence.core.ConfluenceConfig
import ru.kode.android.build.publish.plugin.firebase.core.FirebaseAppDistributionConfig
import ru.kode.android.build.publish.plugin.jira.core.JiraConfig
import ru.kode.android.build.publish.plugin.extension.config.OutputConfig
import ru.kode.android.build.publish.plugin.play.core.PlayConfig
import ru.kode.android.build.publish.plugin.slack.core.SlackConfig
import ru.kode.android.build.publish.plugin.telegram.core.TelegramConfig
import javax.inject.Inject

const val EXTENSION_NAME = "buildPublish"

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val output: NamedDomainObjectContainer<OutputConfig> =
            objectFactory.domainObjectContainer(OutputConfig::class.java)

        val changelog: NamedDomainObjectContainer<ChangelogConfig> =
            objectFactory.domainObjectContainer(ChangelogConfig::class.java)

        val jira: NamedDomainObjectContainer<JiraConfig> =
            objectFactory.domainObjectContainer(JiraConfig::class.java)

        val clickUp: NamedDomainObjectContainer<ClickUpConfig> =
            objectFactory.domainObjectContainer(ClickUpConfig::class.java)

        val telegram: NamedDomainObjectContainer<TelegramConfig> =
            objectFactory.domainObjectContainer(TelegramConfig::class.java)

        val confluence: NamedDomainObjectContainer<ConfluenceConfig> =
            objectFactory.domainObjectContainer(ConfluenceConfig::class.java)

        val slack: NamedDomainObjectContainer<SlackConfig> =
            objectFactory.domainObjectContainer(SlackConfig::class.java)

        val firebaseDistribution: NamedDomainObjectContainer<FirebaseAppDistributionConfig> =
            objectFactory.domainObjectContainer(FirebaseAppDistributionConfig::class.java)

        val appCenterDistribution: NamedDomainObjectContainer<AppCenterDistributionConfig> =
            objectFactory.domainObjectContainer(AppCenterDistributionConfig::class.java)

        val play: NamedDomainObjectContainer<PlayConfig> =
            objectFactory.domainObjectContainer(PlayConfig::class.java)
    }
