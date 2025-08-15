package ru.kode.android.build.publish.plugin.telegram.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrCommon
import ru.kode.android.build.publish.plugin.telegram.config.TelegramChangelogConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramDistributionConfig
import ru.kode.android.build.publish.plugin.telegram.service.TelegramServiceExtension
import ru.kode.android.build.publish.plugin.telegram.task.changelog.SendTelegramChangelogTask
import ru.kode.android.build.publish.plugin.telegram.task.distribution.TelegramDistributionTask

internal const val SEND_TELEGRAM_CHANGELOG_TASK_PREFIX = "sendTelegramChangelog"
internal const val TELEGRAM_DISTRIBUTION_UPLOAD_TASK_PREFIX = "telegramDistributionUpload"

internal object TelegramTasksRegistrar {
    private val logger: Logger = Logging.getLogger(this::class.java)

    internal fun registerChangelogTask(
        project: Project,
        changelogConfig: TelegramChangelogConfig,
        params: TelegramChangelogTaskParams,
    ): TaskProvider<SendTelegramChangelogTask> {
        return project.registerSendTelegramChangelogTask(changelogConfig, params)
    }

    internal fun registerDistributionTask(
        project: Project,
        distributionConfig: TelegramDistributionConfig,
        params: TelegramDistributionTaskParams,
    ): TaskProvider<TelegramDistributionTask>? {
        return if (distributionConfig.destinationBots.isPresent) {
            project.registerTelegramUploadTask(distributionConfig, params)
        } else {
            logger.info(
                "TelegramDistributionTask was not created, destinationBots is not present",
            )
            null
        }
    }
}

private fun Project.registerSendTelegramChangelogTask(
    changelogConfig: TelegramChangelogConfig,
    params: TelegramChangelogTaskParams,
): TaskProvider<SendTelegramChangelogTask> {
    return tasks.register(
        "$SEND_TELEGRAM_CHANGELOG_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        SendTelegramChangelogTask::class.java,
    ) {
        val networkService =
            project.extensions
                .getByType(TelegramServiceExtension::class.java)
                .networkServices
                .flatMapByNameOrCommon(params.buildVariant.name)

        it.changelogFile.set(params.changelogFile)
        it.buildTagFile.set(params.lastBuildTagFile)
        it.issueUrlPrefix.set(params.issueUrlPrefix)
        it.issueNumberPattern.set(params.issueNumberPattern)
        it.baseOutputFileName.set(params.baseFileName)
        it.userMentions.set(changelogConfig.userMentions)
        it.destinationBots.set(changelogConfig.destinationBots)
        it.networkService.set(networkService)
    }
}

@Suppress("MaxLineLength") // One parameter function
private fun Project.registerTelegramUploadTask(
    distributionConfig: TelegramDistributionConfig,
    params: TelegramDistributionTaskParams,
): TaskProvider<TelegramDistributionTask> {
    return tasks.register(
        "$TELEGRAM_DISTRIBUTION_UPLOAD_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        TelegramDistributionTask::class.java,
    ) {
        val networkService =
            project.extensions
                .getByType(TelegramServiceExtension::class.java)
                .networkServices
                .flatMapByNameOrCommon(params.buildVariant.name)

        it.distributionFile.set(params.apkOutputFile)
        it.destinationBots.set(distributionConfig.destinationBots)
        it.networkService.set(networkService)
    }
}

internal data class TelegramChangelogTaskParams(
    val baseFileName: Provider<String>,
    val issueNumberPattern: Provider<String>,
    val issueUrlPrefix: Provider<String>,
    val buildVariant: BuildVariant,
    val changelogFile: Provider<RegularFile>,
    val lastBuildTagFile: Provider<RegularFile>,
)

internal data class TelegramDistributionTaskParams(
    val baseFileName: Provider<String>,
    val buildVariant: BuildVariant,
    val lastBuildTag: Provider<RegularFile>,
    val apkOutputFile: Provider<RegularFile>,
)
