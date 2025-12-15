package ru.kode.android.build.publish.plugin.slack.service

import org.gradle.api.provider.Provider

/**
 * Extension class that provides access to configured Slack services.
 *
 * This class serves as a container for both webhook and upload services
 * that can be used to interact with Slack's API. It's typically used as
 * an extension in Gradle build scripts to access configured Slack services.
 */
abstract class SlackServiceExtension(
    /**
     * Provider for a map of named [SlackService] providers.
     * The key is the service name, and the value is a provider for the service.
     */
    val services: Provider<Map<String, Provider<SlackService>>>,
)
