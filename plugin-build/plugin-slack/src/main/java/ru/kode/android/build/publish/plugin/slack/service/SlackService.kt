package ru.kode.android.build.publish.plugin.slack.service

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.entity.IssueSource
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.slack.controller.SlackController
import ru.kode.android.build.publish.plugin.slack.controller.SlackControllerFactory
import ru.kode.android.build.publish.plugin.slack.controller.SlackMessage
import ru.kode.android.build.publish.plugin.slack.messages.uploadApiTokenRequiredMessage
import java.io.File
import javax.inject.Inject

abstract class SlackService
    @Inject
    constructor(
        @Suppress("UNUSED_PARAMETER") providerFactory: ProviderFactory,
    ) : BuildService<SlackService.Params> {
        interface Params : BuildServiceParameters {
            val webhookUrl: Property<String>
            val uploadApiTokenFile: RegularFileProperty
            val loggerService: Property<LoggerService>
        }

        private val controller: SlackController by lazy {
            SlackControllerFactory.build(parameters.loggerService.get().logger)
        }

        fun send(
            initialComment: String,
            changelog: String,
            userMentions: List<String>,
            iconUrl: String,
            attachmentColor: String,
            issueSources: List<IssueSource>,
        ) {
            controller.send(
                SlackMessage(
                    webhookUrl = parameters.webhookUrl.get(),
                    text = changelog,
                    header = initialComment,
                    userMentions = userMentions,
                    iconUrl = iconUrl,
                    attachmentColor = attachmentColor,
                    issueSources = issueSources,
                ),
            )
        }

        fun upload(
            initialComment: String,
            file: File,
            channels: List<String>,
        ) {
            val uploadApiTokenFile =
                parameters.uploadApiTokenFile.orNull
                    ?: throw GradleException(uploadApiTokenRequiredMessage())
            controller.upload(
                uploadToken = uploadApiTokenFile.asFile.readText(),
                initialComment = initialComment,
                file = file,
                channels = channels,
            )
        }
    }
