package ru.kode.android.build.publish.plugin.sender.task.slack.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.slack.controller.SlackControllerFactory
import javax.inject.Inject

internal interface SendSlackFileParameters : WorkParameters {
    val uploadApiToken: Property<String>
    val file: RegularFileProperty
    val channels: SetProperty<String>
    val comment: Property<String>
}

internal abstract class SendSlackFileWork
    @Inject
    constructor() : WorkAction<SendSlackFileParameters> {
        override fun execute() {
            SlackControllerFactory.build().upload(
                uploadToken = parameters.uploadApiToken.get(),
                initialComment = parameters.comment.get(),
                file = parameters.file.asFile.get(),
                channels = parameters.channels.get().toList(),
            )
        }
    }
