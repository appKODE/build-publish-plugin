package ru.kode.android.build.publish.plugin.telegram.service

import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.entity.IssueSource
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramController
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramControllerFactory
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramMessage
import ru.kode.android.build.publish.plugin.telegram.controller.entity.ChatSpecificTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.DestinationTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramLastMessage
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.telegramBotFromJson
import ru.kode.android.build.publish.plugin.telegram.messages.noMatchingConfigurationMessage
import java.io.File
import javax.inject.Inject

abstract class TelegramService
    @Inject
    constructor() : BuildService<TelegramService.Params> {
        interface Params : BuildServiceParameters {
            val bots: ListProperty<String>
            val loggerService: Property<LoggerService>
        }

        private val controller: TelegramController by lazy {
            TelegramControllerFactory.build(parameters.loggerService.get().logger)
        }

        private val bots: List<TelegramBot>
            get() =
                parameters.bots
                    .map { it.map { telegramBotFromJson(it) } }
                    .get()

        fun send(
            changelog: String,
            header: String,
            userMentions: List<String>,
            issueSources: List<IssueSource>,
            destinationBots: List<DestinationTelegramBot>,
        ) {
            controller.send(
                TelegramMessage(
                    text = changelog,
                    bots = bots.mapToChatSpecificBots(destinationBots = destinationBots),
                    header = header,
                    userMentions = userMentions,
                    issueSources = issueSources,
                ),
            )
        }

        fun upload(
            file: File,
            destinationBots: List<DestinationTelegramBot>,
        ) {
            controller.upload(
                file,
                bots.mapToChatSpecificBots(destinationBots = destinationBots),
            )
        }

        fun getLastMessage(
            botName: String,
            chatName: String,
            topicName: String?,
        ): TelegramLastMessage? {
            val bot =
                bots.firstOrNull { it.name == botName }
                    ?: throw GradleException(noMatchingConfigurationMessage(botName))
            return controller.getLastMessage(bot.id, chatName, topicName)
        }
    }

@Suppress("MaxLineLength")
private fun Collection<TelegramBot>.mapToChatSpecificBots(destinationBots: List<DestinationTelegramBot>): List<ChatSpecificTelegramBot> {
    return destinationBots
        .flatMap { destinationBot ->
            val botName = destinationBot.botName
            val bot = this.firstOrNull { it.name == botName }
            bot?.mapToChatSpecificBot(destinationBot)
                ?: throw GradleException(noMatchingConfigurationMessage(destinationBot))
        }
}

@Suppress("TooGenericExceptionThrown")
private fun TelegramBot.mapToChatSpecificBot(destinationBot: DestinationTelegramBot): List<ChatSpecificTelegramBot> {
    val chatNames = destinationBot.chatNames
    return chats.filter { it.name in chatNames }
        .map { chat ->
            val authPassword = this.basicAuth?.password
            val authUserName = this.basicAuth?.username
            val basicAuth =
                if (authUserName != null && authPassword != null) {
                    ChatSpecificTelegramBot.BasicAuth(authUserName, authPassword)
                } else {
                    null
                }
            ChatSpecificTelegramBot(
                name = this.name,
                id = this.id,
                serverBaseUrl = this.serverBaseUrl,
                basicAuth = basicAuth,
                chatId = chat.id,
                topicId = chat.topicId,
            )
        }
}
