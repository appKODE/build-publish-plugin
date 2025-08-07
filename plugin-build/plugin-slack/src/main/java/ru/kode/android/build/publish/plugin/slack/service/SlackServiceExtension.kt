package ru.kode.android.build.publish.plugin.slack.service

import org.gradle.api.provider.Provider

abstract class SlackServiceExtension(
    val webhookServices: Provider<Map<String, Provider<SlackWebhookService>>>,
    val uploadServices: Provider<Map<String, Provider<SlackUploadService>>>,
)
