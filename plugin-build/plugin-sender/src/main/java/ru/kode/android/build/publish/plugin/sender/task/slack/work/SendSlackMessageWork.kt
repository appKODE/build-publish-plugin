package ru.kode.android.build.publish.plugin.sender.task.slack.work

import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.slack.controller.SlackControllerFactory
import ru.kode.android.build.publish.plugin.slack.controller.SlackMessage
import javax.inject.Inject

internal interface SendSlackMessageParameters : WorkParameters {
    val webhookUrl: Property<String>
    val message: Property<String>
    val attachmentColor: Property<String>
    val iconUrl: Property<String>
}

internal abstract class SendSlackMessageWork
    @Inject
    constructor() : WorkAction<SendSlackMessageParameters> {
        override fun execute() {
            SlackControllerFactory.build().send(
                SlackMessage(
                    webhookUrl = parameters.webhookUrl.get(),
                    text = parameters.message.get(),
                    attachmentColor = parameters.attachmentColor.get(),
                    iconUrl = parameters.iconUrl.get(),
                ),
            )
        }
    }
