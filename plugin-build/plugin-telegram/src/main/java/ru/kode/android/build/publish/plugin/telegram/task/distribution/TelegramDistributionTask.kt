package ru.kode.android.build.publish.plugin.telegram.task.distribution

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
import ru.kode.android.build.publish.plugin.telegram.config.DestinationTelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.mapToEntity
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.toJson
import ru.kode.android.build.publish.plugin.telegram.service.TelegramService
import ru.kode.android.build.publish.plugin.telegram.task.distribution.work.TelegramUploadWork
import javax.inject.Inject

/**
 * Gradle task for distributing APK files via Telegram.
 *
 * This task handles the distribution of Android application packages (APKs/bundles) to specified
 * Telegram chats using configured bot tokens. It's designed to be used as part of a
 * CI/CD pipeline to automatically share build artifacts with testers or stakeholders.
 *
 * The task uses Gradle's Worker API to perform the upload asynchronously,
 * which is particularly useful for large files.
 */
abstract class TelegramDistributionTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {

        init {
            description = "Task to send APK/bundle to Telegram"
            group = BasePlugin.BUILD_GROUP
        }

        /**
         * Internal network service for handling Telegram API communication.
         * This is marked as @Internal as it's not part of the task's input/output.
         */
        @get:Internal
        abstract val service: Property<TelegramService>

        /**
         * The APK/bundle file to be distributed.
         *
         * This property is marked as an input file, so Gradle will check for changes
         * and only run the task if the file has been modified.
         */
        @get:InputFile
        @get:Option(
            option = "distributionFile",
            description = "Absolute path to the file to be distributed",
        )
        abstract val distributionFile: RegularFileProperty

        /**
         * Set of configured Telegram bots and their destination chats.
         *
         * Each [DestinationTelegramBotConfig] contains the bot and chat names where the file should be sent.
         * The task will send the file to all specified destinations.
         */
        @get:Input
        @get:Option(
            option = "destinationBots",
            description = "List of Telegram bot configurations for distribution",
        )
        abstract val destinationBots: Property<String>

        /**
         * Task action that handles the APK/bundle upload to Telegram.
         *
         * This method is automatically called by Gradle when the task is executed.
         * It creates a new worker to handle the upload asynchronously.
         *
         * The actual upload work is delegated to [TelegramUploadWork] to maintain
         * clean task boundaries and enable better build caching.
         */
        @TaskAction
        fun upload() {
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(TelegramUploadWork::class.java) { parameters ->
                parameters.distributionFile.set(distributionFile)
                parameters.service.set(service)
                parameters.destinationBots.set(destinationBots)
            }
        }
    }
