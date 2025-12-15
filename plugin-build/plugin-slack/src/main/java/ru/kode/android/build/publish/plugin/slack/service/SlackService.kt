package ru.kode.android.build.publish.plugin.slack.service

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.core.util.UploadError
import ru.kode.android.build.publish.plugin.slack.controller.SlackController
import ru.kode.android.build.publish.plugin.slack.controller.SlackControllerImpl
import ru.kode.android.build.publish.plugin.slack.network.SlackApi
import ru.kode.android.build.publish.plugin.slack.network.SlackUploadApi
import ru.kode.android.build.publish.plugin.slack.network.factory.SlackClientFactory
import ru.kode.android.build.publish.plugin.slack.network.factory.SlackRetrofitBuilderFactory
import ru.kode.android.build.publish.plugin.slack.network.factory.SlackUploadApiFactory
import ru.kode.android.build.publish.plugin.slack.network.factory.SlackWebhookApiFactory
import java.io.File
import javax.inject.Inject

/**
 * A network service for interacting with the Slack API.
 *
 * This service provides the underlying HTTP client and Retrofit configuration
 * for Slack API communication. It's designed to be used as a shared service
 * across different Slack-related tasks to manage resources efficiently.
 */
abstract class SlackService
    @Inject
    constructor(
        providerFactory: ProviderFactory,
    ) : BuildService<SlackService.Params> {

        interface Params : BuildServiceParameters {
            /**
             * The incoming webhook URL for sending messages
             */
            val webhookUrl: Property<String>
            /**
             * File containing the Slack API token for file uploads
             */
            val uploadApiTokenFile: RegularFileProperty
        }

        private val logger: Logger = Logging.getLogger("Slack")

        internal abstract val okHttpClientProperty: Property<OkHttpClient>
        internal abstract val retrofitProperty: Property<Retrofit.Builder>
        internal abstract val jsonProperty: Property<Json>
        internal abstract val apiProperty: Property<SlackApi>
        internal abstract val uploadApiProperty: Property<SlackUploadApi>
        internal abstract val controllerProperty: Property<SlackController>

        init {
            okHttpClientProperty.set(
                SlackClientFactory.build(logger)
            )
            jsonProperty.set(
                providerFactory.provider { Json { ignoreUnknownKeys = true } }
            )
            retrofitProperty.set(
                okHttpClientProperty.zip(jsonProperty) { client, json ->
                    SlackRetrofitBuilderFactory.build(client, json)
                },
            )
            apiProperty.set(
                retrofitProperty.map { retrofit ->
                    SlackWebhookApiFactory.build(retrofit)
                },
            )
            uploadApiProperty.set(
                retrofitProperty.map { retrofit ->
                    SlackUploadApiFactory.build(retrofit)
                },
            )
            controllerProperty.set(
                jsonProperty.flatMap { json ->
                    apiProperty.zip(uploadApiProperty) { webhookApi, distributionApi ->
                        SlackControllerImpl(json, webhookApi, distributionApi, logger)
                    }
                }
            )
        }

        private val controller: SlackController get() = controllerProperty.get()


        /**
         * Sends a formatted message to the configured Slack webhook.
         *
         * This method:
         * 1. Formats the message with the provided parameters
         * 2. Handles message chunking if it exceeds Slack's size limits
         * 3. Sends the message to the configured webhook URL
         *
         * @param initialComment The base name for the build being reported
         * @param changelog The changelog text to include in the message
         * @param userMentions Optional user mentions to include in the message
         * @param iconUrl URL of the icon to display with the message
         * @param attachmentColor Color code for the message attachment (e.g., "#36a64f" for green)
         *
         * @throws Exception if the webhook request fails
         */
        fun send(
            initialComment: String,
            changelog: String,
            userMentions: List<String>,
            iconUrl: String,
            attachmentColor: String,
            issueUrlPrefix: String,
            issueNumberPattern: String
        ) {
            controller.send(
                webhookUrl = parameters.webhookUrl.get(),
                initialComment = initialComment,
                changelog = changelog,
                userMentions = userMentions,
                iconUrl = iconUrl,
                attachmentColor = attachmentColor,
                issueUrlPrefix = issueUrlPrefix,
                issueNumberPattern = issueNumberPattern,
            )
        }

        /**
         * Uploads a file to Slack and shares it in the specified channels.
         *
         * This method performs a multi-step upload process:
         * 1. Requests an upload URL from Slack
         * 2. Uploads the file content to the provided URL
         * 3. Completes the upload and shares the file in the specified channels
         *
         * @param initialComment The build name or identifier to include in the comment
         * @param file The file to upload
         * @param channels Set of channel IDs or names where the file should be shared
         *
         * @throws UploadError if any step of the upload process fails
         */
        fun upload(
            initialComment: String,
            file: File,
            channels: List<String>,
        ) {
            controller.upload(
                uploadToken = parameters.uploadApiTokenFile.get().asFile.readText(),
                initialComment = initialComment,
                file = file,
                channels = channels,
            )
        }
    }
