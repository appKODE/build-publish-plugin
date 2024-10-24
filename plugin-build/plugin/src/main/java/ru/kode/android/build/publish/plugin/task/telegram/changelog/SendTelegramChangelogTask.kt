package ru.kode.android.build.publish.plugin.task.telegram.changelog

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.enity.mapper.fromJson
import ru.kode.android.build.publish.plugin.task.telegram.changelog.work.SendTelegramChangelogWork
import javax.inject.Inject

abstract class SendTelegramChangelogTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Task to send changelog for Telegram"
            group = BasePlugin.BUILD_GROUP
        }

        @get:InputFile
        @get:Option(option = "changelogFile", description = "File with saved changelog")
        abstract val changelogFile: RegularFileProperty

        @get:InputFile
        @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
        abstract val tagBuildFile: RegularFileProperty

        @get:Input
        @get:Option(
            option = "baseOutputFileName",
            description = "Application bundle name for changelog",
        )
        abstract val baseOutputFileName: Property<String>

        @get:Input
        @get:Option(
            option = "issueUrlPrefix",
            description = "Address of task tracker",
        )
        abstract val issueUrlPrefix: Property<String>

        @get:Input
        @get:Option(
            option = "issueNumberPattern",
            description = "How task number formatted",
        )
        abstract val issueNumberPattern: Property<String>

        @get:Input
        @get:Option(option = "botId", description = "Bot id where webhook posted")
        abstract val botId: Property<String>

        @get:Input
        @get:Option(option = "chatId", description = "Chat id where webhook posted")
        abstract val chatId: Property<String>

        @get:Input
        @get:Optional
        @get:Option(option = "topicId", description = "Unique identifier for the target message thread")
        abstract val topicId: Property<String>

        @get:Input
        @get:Option(option = "userMentions", description = "User tags to mention in chat")
        abstract val userMentions: SetProperty<String>

        @TaskAction
        fun sendChangelog() {
            val currentBuildTag = fromJson(tagBuildFile.asFile.get())
            val escapedCharacters =
                "[_]|[*]|[\\[]|[\\]]|[(]|[)]|[~]|[`]|[>]|[#]|[+]|[=]|[|]|[{]|[}]|[.]|[!]|-"
            val changelog = changelogFile.orNull?.asFile?.readText()
            if (changelog.isNullOrEmpty()) {
                logger.error(
                    "[sendChangelog] changelog file not found, is empty or error occurred",
                )
            } else {
                val changelogWithIssues = changelog.formatIssues(escapedCharacters)
                val userMentions =
                    userMentions.orNull.orEmpty().joinToString(", ")
                        .replace(escapedCharacters.toRegex()) { result -> "\\${result.value}" }
                val workQueue: WorkQueue = workerExecutor.noIsolation()
                if (changelogWithIssues.length > MESSAGE_MAX_LENGTH) {
                    changelogWithIssues
                        .chunked(MESSAGE_MAX_LENGTH)
                        .forEach { chunk ->
                            sendChangelogInternal(
                                workQueue = workQueue,
                                userMentions = userMentions,
                                escapedCharacters = escapedCharacters,
                                changelog = chunk,
                                currentBuildTagName = currentBuildTag.name,
                            )
                        }
                } else {
                    sendChangelogInternal(
                        workQueue = workQueue,
                        userMentions = userMentions,
                        escapedCharacters = escapedCharacters,
                        changelog = changelogWithIssues,
                        currentBuildTagName = currentBuildTag.name,
                    )
                }
            }
        }

        private fun sendChangelogInternal(
            workQueue: WorkQueue,
            userMentions: String,
            escapedCharacters: String,
            changelog: String,
            currentBuildTagName: String,
        ) {
            workQueue.submit(SendTelegramChangelogWork::class.java) { parameters ->
                parameters.baseOutputFileName.set(baseOutputFileName)
                parameters.buildName.set(currentBuildTagName)
                parameters.changelog.set(changelog)
                parameters.userMentions.set(userMentions)
                parameters.escapedCharacters.set(escapedCharacters)
                parameters.botId.set(botId)
                parameters.chatId.set(chatId)
                parameters.topicId.set(topicId)
            }
        }

        private fun String.formatIssues(escapedCharacters: String): String {
            val issueUrlPrefix = issueUrlPrefix.get()
            val issueNumberPattern = issueNumberPattern.get()
            val issueRegexp = issueNumberPattern.toRegex()

            val matchResults = issueRegexp.findAll(this).distinctBy { it.value }
            var out = this.escapeCharacters(escapedCharacters)

            matchResults.forEach { matchResult ->
                val formattedResult = matchResult.value.escapeCharacters(escapedCharacters)
                val url = (issueUrlPrefix + matchResult.value).escapeCharacters(escapedCharacters)
                val issueId = matchResult.value.escapeCharacters(escapedCharacters)
                val link = "[$issueId]($url)"
                out = out.replace(formattedResult, link)
            }
            return out
        }

        private fun String.escapeCharacters(escapedCharacters: String): String {
            return this.replace(escapedCharacters.toRegex()) { result -> "\\${result.value}" }
        }

        private fun String.chunked(
            chunkLength: Int,
            delimiter: Char = '\n',
        ): List<String> {
            val result = mutableListOf<String>()
            var currentIndex = 0
            while (currentIndex < this.length) {
                var nextNewlineIndex = currentIndex
                var tempNewlineIndex = currentIndex
                while (tempNewlineIndex < (currentIndex + chunkLength)) {
                    tempNewlineIndex = this.indexOf(delimiter, tempNewlineIndex + 1)
                    if (tempNewlineIndex == -1) {
                        val chunk = this.substring(currentIndex, nextNewlineIndex)
                        result.add(chunk)
                        return result
                    }
                    if (tempNewlineIndex <= (currentIndex + chunkLength)) {
                        nextNewlineIndex = tempNewlineIndex
                    }
                }
                val chunk = this.substring(currentIndex, nextNewlineIndex)
                result.add(chunk)
                currentIndex = nextNewlineIndex
            }
            return result
        }
    }

private const val MESSAGE_MAX_LENGTH = 4096
