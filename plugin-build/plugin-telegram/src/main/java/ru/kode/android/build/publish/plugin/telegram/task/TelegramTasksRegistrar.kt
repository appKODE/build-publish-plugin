package ru.kode.android.build.publish.plugin.telegram.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrDefault
import ru.kode.android.build.publish.plugin.telegram.core.TelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.core.TelegramChangelogConfig
import ru.kode.android.build.publish.plugin.telegram.core.TelegramDistributionConfig
import ru.kode.android.build.publish.plugin.telegram.service.TelegramNetworkServiceExtension
import ru.kode.android.build.publish.plugin.telegram.task.changelog.SendTelegramChangelogTask
import ru.kode.android.build.publish.plugin.telegram.task.distribution.TelegramDistributionTask

internal const val SEND_TELEGRAM_CHANGELOG_TASK_PREFIX = "sendTelegramChangelog"
internal const val TELEGRAM_DISTRIBUTION_UPLOAD_TASK_PREFIX = "telegramDistributionUpload"

object TelegramTasksRegistrar {

    fun registerChangelogTask(
        project: Project,
        changelogConfig: TelegramChangelogConfig,
        params: TelegramChangelogTaskParams
    ): TaskProvider<SendTelegramChangelogTask> {
        return project.registerSendTelegramChangelogTask(changelogConfig, params)
    }

    fun registerDistributionTask(
        project: Project,
        distributionConfig: TelegramDistributionConfig,
        params: TelegramDistributionTasksParams
    ): TaskProvider<TelegramDistributionTask>? {
        return if (distributionConfig.uploadBuild.orNull == true) {
            project.registerTelegramUploadTask(params)
        } else {
            // TODO: Add logs
            null
        }
    }
}

private fun Project.registerSendTelegramChangelogTask(
    changelogConfig: TelegramChangelogConfig,
    params: TelegramChangelogTaskParams
): TaskProvider<SendTelegramChangelogTask> {
    return tasks.register(
        "$SEND_TELEGRAM_CHANGELOG_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        SendTelegramChangelogTask::class.java,
    ) {

        val networkService = project.extensions
            .getByType(TelegramNetworkServiceExtension::class.java)
            .services
            .flatMapByNameOrDefault(params.buildVariant.name)

        it.changelogFile.set(params.generateChangelogFileProvider)
        it.tagBuildFile.set(params.tagBuildProvider)
        it.issueUrlPrefix.set(params.issueUrlPrefix)
        it.issueNumberPattern.set(params.issueNumberPattern)
        it.baseOutputFileName.set(params.baseFileName)
        it.userMentions.set(changelogConfig.userMentions)
        it.networkService.set(networkService)
    }
}

private fun Project.registerTelegramUploadTask(
    params: TelegramDistributionTasksParams,
): TaskProvider<TelegramDistributionTask> {
    return tasks.register(
        "$TELEGRAM_DISTRIBUTION_UPLOAD_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        TelegramDistributionTask::class.java,
    ) {
        val networkService = project.extensions
            .getByType(TelegramNetworkServiceExtension::class.java)
            .services
            .flatMapByNameOrDefault(params.buildVariant.name)

        it.buildVariantOutputFile.set(params.apkOutputFileProvider)
        it.networkService.set(networkService)
    }
}

data class TelegramChangelogTaskParams(
    val baseFileName: Property<String>,
    val issueNumberPattern: Provider<String>,
    val issueUrlPrefix: Property<String>,
    val buildVariant: BuildVariant,
    val generateChangelogFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
)

data class TelegramDistributionTasksParams(
    val baseFileName: Property<String>,
    val buildVariant: BuildVariant,
    val tagBuildProvider: Provider<RegularFile>,
    val apkOutputFileProvider: Provider<RegularFile>,
)
