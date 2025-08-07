package ru.kode.android.build.publish.plugin.slack.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.slack.service.upload.SlackUploadService
import ru.kode.android.build.publish.plugin.slack.service.webhook.SlackWebhookService

abstract class SlackServiceExtension(
    val webhookServices: Provider<Map<String, Provider<SlackWebhookService>>>,
    val uploadServices: Provider<Map<String, Provider<SlackUploadService>>>,
)
