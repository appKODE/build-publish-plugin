package ru.kode.android.build.publish.plugin.telegram.core

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.telegram.task.changelog.SendTelegramChangelogTask
import ru.kode.android.build.publish.plugin.telegram.task.distribution.TelegramDistributionTask

internal const val SEND_TELEGRAM_CHANGELOG_TASK_PREFIX = "sendTelegramChangelog"
internal const val TELEGRAM_DISTRIBUTION_UPLOAD_TASK_PREFIX = "telegramDistributionUpload"

interface TelegramConfig {
    val name: String

    /**
     * Telegram bot id to post changelog in chat
     */
    @get:Input
    val botId: Property<String>

    /**
     * Bot server base url
     */
    @get:Input
    @get:Optional
    val botBaseUrl: Property<String>

    /**
     * Bot server auth username
     */
    @get:Input
    @get:Optional
    val botAuthUsername: Property<String>

    /**
     * Bot server auth password
     */
    @get:Input
    @get:Optional
    val botAuthPassword: Property<String>

    /**
     * Telegram chat id where changelog will be posted
     */
    @get:Input
    val chatId: Property<String>

    /**
     * Unique identifier for the target message thread
     * Represents "message_thread_id"
     */
    @get:Input
    @get:Optional
    val topicId: Property<String>

    /**
     * List of mentioning users for Slack, can be empty or null
     * For example: ["@aa", "@bb", "@ccc"]
     */
    @get:Input
    val userMentions: SetProperty<String>

    /**
     * Should upload build at the same chat or not
     * Works only if file size is smaller then 50 mb
     */
    @get:Input
    @get:Optional
    val uploadBuild: Property<Boolean>

    fun registerChangelogTask(
        project: Project,
        params: TelegramChangelogTaskParams
    ): TaskProvider<SendTelegramChangelogTask> {
        return project.tasks.registerSendTelegramChangelogTask(this, params)
    }

    fun registerDistributionTask(
        project: Project,
        params: TelegramDistributionTasksParams
    ): TaskProvider<TelegramDistributionTask>? {
        return if (this.uploadBuild.orNull == true) {
            project.tasks.registerTelegramUploadTask(this, params)
        } else {
            // TODO: Add logs
            null
        }
    }
}

private fun TaskContainer.registerSendTelegramChangelogTask(
    config: TelegramConfig,
    params: TelegramChangelogTaskParams
): TaskProvider<SendTelegramChangelogTask> {
    return register(
        "$SEND_TELEGRAM_CHANGELOG_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        SendTelegramChangelogTask::class.java,
    ) {
        it.changelogFile.set(params.generateChangelogFileProvider)
        it.tagBuildFile.set(params.tagBuildProvider)
        it.issueUrlPrefix.set(params.issueUrlPrefix)
        it.issueNumberPattern.set(params.issueNumberPattern)
        it.baseOutputFileName.set(params.baseFileName)
        it.botId.set(config.botId)
        it.chatId.set(config.chatId)
        it.topicId.set(config.topicId)
        it.userMentions.set(config.userMentions)
    }
}

private fun TaskContainer.registerTelegramUploadTask(
    config: TelegramConfig,
    params: TelegramDistributionTasksParams,
): TaskProvider<TelegramDistributionTask> {
    return register(
        "$TELEGRAM_DISTRIBUTION_UPLOAD_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        TelegramDistributionTask::class.java,
    ) {
        it.buildVariantOutputFile.set(params.apkOutputFileProvider)
        it.botId.set(config.botId)
        it.chatId.set(config.chatId)
        it.topicId.set(config.topicId)
    }
}

data class TelegramChangelogTaskParams(
    val baseFileName: Property<String>,
    val issueNumberPattern: Property<String>,
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
