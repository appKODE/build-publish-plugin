package ru.kode.android.build.publish.plugin.slack.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.slack.config.SlackBotConfig
import ru.kode.android.build.publish.plugin.slack.config.SlackChangelogConfig
import ru.kode.android.build.publish.plugin.slack.config.SlackDistributionConfig
import ru.kode.android.build.publish.plugin.slack.task.SlackChangelogTaskParams
import ru.kode.android.build.publish.plugin.slack.task.SlackDistributionTaskParams
import ru.kode.android.build.publish.plugin.slack.task.SlackTasksRegistrar
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishSlackExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
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

        override fun configure(
            project: Project,
            input: ExtensionInput,
        ) {
            val slackBotConfig = botConfig(input.buildVariant.name)
            val slackChangelogConfig = changelogConfigOrNull(input.buildVariant.name)
            val slackDistributionConfig = distributionConfigOrNull(input.buildVariant.name)

            if (slackChangelogConfig != null) {
                SlackTasksRegistrar.registerChangelogTask(
                    project = project,
                    botConfig = slackBotConfig,
                    changelogConfig = slackChangelogConfig,
                    params =
                        SlackChangelogTaskParams(
                            baseFileName = input.output.baseFileName,
                            issueNumberPattern = input.changelog.issueNumberPattern,
                            issueUrlPrefix = input.changelog.issueUrlPrefix,
                            buildVariant = input.buildVariant,
                            changelogFile = input.changelog.file,
                            lastBuildTagFile = input.output.lastBuildTagFile,
                        ),
                )
            }

            if (slackDistributionConfig != null) {
                SlackTasksRegistrar.registerDistributionTask(
                    project = project,
                    distributionConfig = slackDistributionConfig,
                    params =
                        SlackDistributionTaskParams(
                            baseFileName = input.output.baseFileName,
                            buildVariant = input.buildVariant,
                            lastBuildTagFile = input.output.lastBuildTagFile,
                            apkOutputFile = input.output.apkFile,
                        ),
                )
            }
        }
    }
