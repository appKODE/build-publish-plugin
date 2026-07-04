package ru.kode.android.build.publish.plugin.slack.task.standalone.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import ru.kode.android.build.publish.plugin.slack.messages.uploadingSlackFileMessage
import ru.kode.android.build.publish.plugin.slack.service.SlackService
import javax.inject.Inject

internal interface SendSlackFileParameters : ServiceWorkParameters {
    val file: RegularFileProperty
    val channels: SetProperty<String>
    val comment: Property<String>
    val service: Property<SlackService>
}

internal abstract class SendSlackFileWork
    @Inject
    constructor() : WorkAction<SendSlackFileParameters> {
        override fun execute() {
            parameters.loggerService.get().info(uploadingSlackFileMessage())
            parameters.service.get().upload(
                initialComment = parameters.comment.get(),
                file = parameters.file.asFile.get(),
                channels = parameters.channels.get().toList(),
            )
        }
    }
