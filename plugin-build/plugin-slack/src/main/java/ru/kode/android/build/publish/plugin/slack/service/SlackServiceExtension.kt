package ru.kode.android.build.publish.plugin.slack.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.slack.service.upload.SlackUploadService
import ru.kode.android.build.publish.plugin.slack.service.webhook.SlackWebhookService

/**
 * Extension class that provides access to configured Slack services.
 *
 * This class serves as a container for both webhook and upload services
 * that can be used to interact with Slack's API. It's typically used as
 * an extension in Gradle build scripts to access configured Slack services.
 */
abstract class SlackServiceExtension(
    /**
     * Provider for a map of named [SlackWebhookService] providers.
     * The key is the service name, and the value is a provider for the service.
     */
    val webhookServices: Provider<Map<String, Provider<SlackWebhookService>>>,
    /**
     * Provider for a map of named [SlackUploadService] providers.
     * The key is the service name, and the value is a provider for the service.
     */
    val uploadServices: Provider<Map<String, Provider<SlackUploadService>>>,
)
