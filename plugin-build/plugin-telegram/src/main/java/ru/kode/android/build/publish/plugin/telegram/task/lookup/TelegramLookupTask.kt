package ru.kode.android.build.publish.plugin.telegram.task.lookup

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.telegram.messages.configErrorExceptionMessage
import ru.kode.android.build.publish.plugin.telegram.messages.configErrorMessage
import ru.kode.android.build.publish.plugin.telegram.messages.lookupSuccessMessage
import ru.kode.android.build.publish.plugin.telegram.service.TelegramService

/**
 * A Gradle task for looking up the last message in a Telegram chat.
 *
 * This task handles the lookup of the last message in a specified Telegram chat using a configured bot token.
 * The task uses Gradle's Worker API to perform the lookup asynchronously.
 */
abstract class TelegramLookupTask : DefaultTask() {
    init {
        description = "Lookup last message in Telegram chat"
        group = BasePlugin.BUILD_GROUP
    }

    /**
     * Internal network service for handling Telegram API communication.
     * This is marked as @Internal as it's not part of the task's input/output.
     */
    @get:Internal
    abstract val service: Property<TelegramService>

    /**
     * The logger service property provides access to the logger service used for logging messages during task execution.
     *
     * This property is marked as @Internal as it's not part of the task's input/output.
     *
     * @see LoggerService
     */
    @get:ServiceReference
    abstract val loggerService: Property<LoggerService>

    /**
     * Name of the Telegram bot configuration to lookup messages in.
     *
     * This property specifies the name of the Telegram bot configuration to lookup messages in.
     * The bot name must match the name of an existing bot configuration to be included in the lookup.
     *
     * @return the name of the Telegram bot configuration to lookup messages in.
     */
    @get:Input
    @get:Option(
        option = "botName",
        description = "Name of the Telegram bot configuration to lookup messages in",
    )
    abstract val botName: Property<String>

    /**
     * Name of the Telegram chat to lookup messages in.
     *
     * This property specifies the name of the Telegram chat to lookup messages in.
     * The chat name must match the name of an existing chat to be included in the lookup.
     *
     * @return the name of the Telegram chat to lookup messages in.
     */
    @get:Input
    @get:Option(
        option = "chatName",
        description = "Name of the Telegram chat to lookup messages in",
    )
    abstract val chatName: Property<String>

    /**
     * Name of the Telegram topic to lookup messages in.
     *
     * If provided, only messages associated with the specified topic will be included in the lookup.
     * If not provided, messages from all topics will be included.
     */
    @get:Input
    @get:Option(
        option = "topicName",
        description = "Name of the Telegram topic to lookup messages in",
    )
    @get:Optional
    abstract val topicName: Property<String>

    /**
     * Performs the lookup of the last message in the specified Telegram chat.
     *
     * This is the action that is executed when the task is run. It retrieves the last message from the specified
     * Telegram chat using the provided [service] and [botName], [chatName], and [topicName] properties.
     *
     * @see TelegramService.getLastMessage
     */
    @TaskAction
    fun lookup() {
        val service = service.get()
        val botName = botName.get()
        val chatName = chatName.get()
        val topicName = topicName.orNull
        val lastMessage = service.getLastMessage(botName, chatName, topicName)
        if (lastMessage == null) {
            loggerService.get().quiet(configErrorMessage(chatName))
            throw GradleException(configErrorExceptionMessage(chatName))
        } else {
            loggerService.get().quiet(lookupSuccessMessage(botName, lastMessage))
        }
    }
}
