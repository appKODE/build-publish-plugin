package ru.kode.android.build.publish.plugin.slack.task.changelog

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.slack.service.webhook.SlackWebhookService
import ru.kode.android.build.publish.plugin.slack.task.changelog.work.SendSlackChangelogWork
import javax.inject.Inject

/**
 * A Gradle task that sends changelog notifications to Slack.
 *
 * This task is responsible for:
 * - Reading changelog content from a file
 * - Formatting the changelog with issue references
 * - Sending the formatted changelog to a Slack channel via webhook
 * - Supporting user mentions and custom formatting
 *
 * The actual notification is sent asynchronously by a Gradle worker to avoid
 * blocking the main build thread.
 */
abstract class SendSlackChangelogTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Task to send changelog to Slack"
            group = BasePlugin.BUILD_GROUP
        }

        /**
         * A property that holds a reference to the SlackWebhookService,
         * which is used for sending notifications to Slack.
         *
         * This property is internal and should only be used by the plugin's own code.
         *
         * @see SlackWebhookService
         */
        @get:Internal
        abstract val networkService: Property<SlackWebhookService>

        /**
         * A property that holds a reference to the changelog file.
         *
         * This file is expected to contain the changelog content.
         *
         * @see SendSlackChangelogTask
         */
        @get:InputFile
        @get:Option(
            option = "changelogFile",
            description = "File with saved changelog",
        )
        abstract val changelogFile: RegularFileProperty

        /**
         * A property that holds a reference to the file containing information about the build tag.
         *
         * This file is expected to contain JSON with information about
         * the last build tag that matches the build pattern.
         *
         * @see SendSlackChangelogTask
         */
        @get:InputFile
        @get:Option(
            option = "buildTagFile",
            description = "Json contains info about tag build",
        )
        abstract val buildTagFile: RegularFileProperty

        /**
         * A property that holds a reference to the base output file name.
         *
         * This is the base name of the output file (e.g., "MyApp") that
         * is used to generate the full file name for the APK artifact.
         * It is concatenated with the build variant name to form the final file name.
         *
         * Example: If `baseOutputFileName` is set to "MyApp", and the build variant is "debug",
         * the final file name will be "MyApp-debug.apk".
         *
         * @see SendSlackChangelogTask
         */
        @get:Input
        @get:Option(
            option = "baseOutputFileName",
            description = "Application bundle name for changelog",
        )
        abstract val baseOutputFileName: Property<String>

        /**
         * A property that holds a reference to the issue URL prefix.
         *
         * This URL prefix is used to generate links to the task tracker system.
         * It is concatenated with the issue number to form the full URL.
         *
         * Example: If `issueUrlPrefix` is set to "https://example.com/issues/",
         * and the issue number is "123", the full URL will be "https://example.com/issues/123".
         *
         * @see SendSlackChangelogTask
         */
        @get:Input
        @get:Option(
            option = "issueUrlPrefix",
            description = "Address of task tracker",
        )
        abstract val issueUrlPrefix: Property<String>

        /**
         * A property that holds a reference to the regular expression pattern used
         * to extract issue numbers from the changelog.
         *
         * This pattern should include a capturing group that matches the full issue
         * key (e.g., "([A-Z]+-\\d+)" for issue keys like "PROJECT-123").
         *
         * The first capturing group will be used as the issue number when formatting
         * the changelog with issue references.
         *
         * @see SendSlackChangelogTask
         */
        @get:Input
        @get:Option(
            option = "issueNumberPattern",
            description = "Regular expression pattern to extract issue numbers from changelog",
        )
        abstract val issueNumberPattern: Property<String>

        /**
         * A property that holds a reference to the icon URL used to identify the app in the chat.
         *
         * This URL should be a valid HTTPS URL pointing to an image file.
         *
         * @see SendSlackChangelogTask
         */
        @get:Input
        @get:Option(
            option = "iconUrl",
            description = "Icon url to show in chat",
        )
        abstract val iconUrl: Property<String>

        /**
         * A property that holds a reference to the list of mentioning users for Slack.
         *
         * This list should contain the Slack mention strings (e.g., "@username", "@here", "@group-name")
         * of the users that should be mentioned in the notification.
         *
         * @see SendSlackChangelogTask
         */
        @get:Input
        @get:Option(
            option = "userMentions",
            description = "List of mentioning users for Slack",
        )
        abstract val userMentions: SetProperty<String>

        /**
         * A property that holds a reference to the attachment's vertical line color in hex format.
         *
         * This color is used to customize the appearance of the Slack message attachment.
         * It should be a valid hex color code (e.g., "#FF0000" for red).
         *
         * @see SendSlackChangelogTask
         */
        @get:Input
        @get:Option(
            option = "attachmentColor",
            description = "Attachment's vertical line color in hex format",
        )
        abstract val attachmentColor: Property<String>

        /**
         * Executes the changelog sending process.
         *
         * This method:
         * 1. Reads the build tag information
         * 2. Formats the changelog with issue references
         * 3. Submits the notification work to a worker thread
         * 4. Handles errors for missing or empty changelog files
         *
         * The actual Slack notification is performed asynchronously by a Gradle worker.
         */
        @TaskAction
        fun sendChangelog() {
            val currentBuildTag = fromJson(buildTagFile.asFile.get())
            val changelog = changelogFile.orNull?.asFile?.readText()
            if (changelog.isNullOrEmpty()) {
                logger.error(
                    "changelog file not found, is empty or error occurred",
                )
            } else {
                val changelogWithIssues = changelog.formatIssues()
                val userMentions = userMentions.orNull.orEmpty().joinToString(", ")
                val workQueue: WorkQueue = workerExecutor.noIsolation()
                workQueue.submit(SendSlackChangelogWork::class.java) { parameters ->
                    parameters.baseOutputFileName.set(baseOutputFileName)
                    parameters.iconUrl.set(iconUrl)
                    parameters.buildName.set(currentBuildTag.name)
                    parameters.changelog.set(changelogWithIssues)
                    parameters.userMentions.set(userMentions)
                    parameters.attachmentColor.set(attachmentColor)
                    parameters.networkService.set(networkService)
                }
            }
        }

        /**
         * Formats issue references in the changelog text.
         *
         * This method transforms raw issue numbers into clickable links using the
         * configured issue URL prefix and pattern.
         *
         * @receiver The raw changelog text
         *
         * @return The formatted changelog with issue links
         */
        private fun String.formatIssues(): String {
            val issueUrlPrefix = issueUrlPrefix.get()
            val issueNumberPattern = issueNumberPattern.get()
            return this
                .replace(Regex(issueNumberPattern), "<$issueUrlPrefix\$0|\$0>")
        }
    }
