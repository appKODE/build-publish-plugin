package ru.kode.android.build.publish.plugin.sender

import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.sender.extension.BuildPublishSenderExtension
import ru.kode.android.build.publish.plugin.sender.task.clickup.AddClickUpFixVersionTask
import ru.kode.android.build.publish.plugin.sender.task.clickup.AddClickUpTagTask
import ru.kode.android.build.publish.plugin.sender.task.confluence.AddConfluenceCommentTask
import ru.kode.android.build.publish.plugin.sender.task.confluence.UploadToConfluenceTask
import ru.kode.android.build.publish.plugin.sender.task.jira.AddJiraFixVersionTask
import ru.kode.android.build.publish.plugin.sender.task.jira.AddJiraLabelTask
import ru.kode.android.build.publish.plugin.sender.task.jira.TransitionJiraIssueTask
import ru.kode.android.build.publish.plugin.sender.task.nextcloud.UploadToNextcloudTask
import ru.kode.android.build.publish.plugin.sender.task.slack.SendSlackFileTask
import ru.kode.android.build.publish.plugin.sender.task.slack.SendSlackMessageTask
import ru.kode.android.build.publish.plugin.sender.task.telegram.SendTelegramFileTask
import ru.kode.android.build.publish.plugin.sender.task.telegram.SendTelegramMessageTask

private const val EXTENSION_NAME = "buildPublishSender"

abstract class BuildPublishSenderPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, BuildPublishSenderExtension::class.java)

        project.afterEvaluate {
            registerTasks(project, extension)
        }
    }

    private fun registerTasks(
        project: Project,
        extension: BuildPublishSenderExtension,
    ) {
        extension.slackConfig?.let { slack ->
            project.tasks.register(TaskNames.Slack.SEND_MESSAGE, SendSlackMessageTask::class.java) { task ->
                task.webhookUrl.set(slack.webhookUrl)
            }
            project.tasks.register(TaskNames.Slack.SEND_FILE, SendSlackFileTask::class.java) { task ->
                task.webhookUrl.set(slack.webhookUrl)
                task.uploadApiTokenFile.set(slack.uploadApiTokenFile)
            }
        }

        extension.telegramConfig?.let { telegram ->
            project.tasks.register(TaskNames.Telegram.SEND_MESSAGE, SendTelegramMessageTask::class.java) { task ->
                task.botId.set(telegram.botId)
                task.chatId.set(telegram.chatId)
                task.topicId.set(telegram.topicId)
                task.serverBaseUrl.set(telegram.serverBaseUrl)
            }
            project.tasks.register(TaskNames.Telegram.SEND_FILE, SendTelegramFileTask::class.java) { task ->
                task.botId.set(telegram.botId)
                task.chatId.set(telegram.chatId)
                task.topicId.set(telegram.topicId)
                task.serverBaseUrl.set(telegram.serverBaseUrl)
            }
        }

        extension.nextcloudConfig?.let { nextcloud ->
            project.tasks.register(TaskNames.Nextcloud.UPLOAD, UploadToNextcloudTask::class.java) { task ->
                task.baseUrl.set(nextcloud.baseUrl)
                task.username.set(nextcloud.username)
                task.password.set(nextcloud.password)
            }
        }

        extension.jiraConfig?.let { jira ->
            project.tasks.register(TaskNames.Jira.ADD_FIX_VERSION, AddJiraFixVersionTask::class.java) { task ->
                task.baseUrl.set(jira.baseUrl)
                task.username.set(jira.username)
                task.apiToken.set(jira.apiToken)
            }
            project.tasks.register(TaskNames.Jira.ADD_LABEL, AddJiraLabelTask::class.java) { task ->
                task.baseUrl.set(jira.baseUrl)
                task.username.set(jira.username)
                task.apiToken.set(jira.apiToken)
            }
            project.tasks.register(TaskNames.Jira.TRANSITION_ISSUE, TransitionJiraIssueTask::class.java) { task ->
                task.baseUrl.set(jira.baseUrl)
                task.username.set(jira.username)
                task.apiToken.set(jira.apiToken)
            }
        }

        extension.confluenceConfig?.let { confluence ->
            project.tasks.register(TaskNames.Confluence.UPLOAD, UploadToConfluenceTask::class.java) { task ->
                task.baseUrl.set(confluence.baseUrl)
                task.username.set(confluence.username)
                task.apiToken.set(confluence.apiToken)
            }
            project.tasks.register(TaskNames.Confluence.ADD_COMMENT, AddConfluenceCommentTask::class.java) { task ->
                task.baseUrl.set(confluence.baseUrl)
                task.username.set(confluence.username)
                task.apiToken.set(confluence.apiToken)
            }
        }

        extension.clickUpConfig?.let { clickUp ->
            project.tasks.register(TaskNames.ClickUp.ADD_TAG, AddClickUpTagTask::class.java) { task ->
                task.apiTokenFile.set(clickUp.apiTokenFile)
            }
            project.tasks.register(TaskNames.ClickUp.ADD_FIX_VERSION, AddClickUpFixVersionTask::class.java) { task ->
                task.apiTokenFile.set(clickUp.apiTokenFile)
            }
        }
    }
}
