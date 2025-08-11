package ru.kode.android.build.publish.plugin.telegram.task.changelog.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.telegram.config.DestinationBot
import ru.kode.android.build.publish.plugin.telegram.service.network.TelegramNetworkService
import javax.inject.Inject

internal interface SendTelegramChangelogParameters : WorkParameters {
    val baseOutputFileName: Property<String>
    val buildName: Property<String>
    val changelog: Property<String>
    val userMentions: Property<String>
    val escapedCharacters: Property<String>
    val networkService: Property<TelegramNetworkService>
    val destinationBots: SetProperty<DestinationBot>
}

internal abstract class SendTelegramChangelogWork
    @Inject
    constructor() : WorkAction<SendTelegramChangelogParameters> {
        private val logger = Logging.getLogger(this::class.java)
        private val service = parameters.networkService.get()

        override fun execute() {
            val baseOutputFileName = parameters.baseOutputFileName.get()
            val buildName = parameters.buildName.get()
            val tgUserMentions = parameters.userMentions.get()
            val escapedHeader =
                "$baseOutputFileName $buildName"
                    .replace(parameters.escapedCharacters.get().toRegex()) { result -> "\\${result.value}" }
            val boldHeader = "*$escapedHeader*"
            val message =
                buildString {
                    append(boldHeader)
                    appendLine()
                    append(tgUserMentions)
                    appendLine()
                    appendLine()
                    append(parameters.changelog.get())
                }.formatChangelog()
            service.send(message, parameters.destinationBots.get())
            logger.info("changelog sent to Telegram")
        }
    }

private fun String.formatChangelog(): String {
    return this
        .replace(Regex("(\r\n|\n)"), "\n")
}
