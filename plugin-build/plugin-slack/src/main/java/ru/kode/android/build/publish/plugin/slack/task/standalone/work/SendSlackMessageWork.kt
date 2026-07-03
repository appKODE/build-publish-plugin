package ru.kode.android.build.publish.plugin.slack.task.standalone.work

import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import ru.kode.android.build.publish.plugin.slack.messages.sendingSlackMessageMessage
import ru.kode.android.build.publish.plugin.slack.service.SlackService
import javax.inject.Inject

internal interface SendSlackMessageParameters : ServiceWorkParameters {
    val message: Property<String>
    val attachmentColor: Property<String>
    val iconUrl: Property<String>
    val service: Property<SlackService>
}

internal abstract class SendSlackMessageWork
    @Inject
    constructor() : WorkAction<SendSlackMessageParameters> {
        override fun execute() {
            parameters.loggerService.get().info(sendingSlackMessageMessage())
            parameters.service.get().send(
                initialComment = "",
                changelog = parameters.message.get(),
                userMentions = emptyList(),
                iconUrl = parameters.iconUrl.get(),
                attachmentColor = parameters.attachmentColor.get(),
                issueSources = emptyList(),
            )
        }
    }
