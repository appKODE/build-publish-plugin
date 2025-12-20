package ru.kode.android.build.publish.plugin.confluence.service

import okhttp3.OkHttpClient
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.confluence.controller.ConfluenceController
import ru.kode.android.build.publish.plugin.confluence.controller.ConfluenceControllerImpl
import ru.kode.android.build.publish.plugin.confluence.network.api.ConfluenceApi
import ru.kode.android.build.publish.plugin.confluence.network.factory.ConfluenceApiFactory
import ru.kode.android.build.publish.plugin.confluence.network.factory.ConfluenceClientFactory
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import java.io.File
import javax.inject.Inject

/**
 * A network service for interacting with the Confluence REST API.
 *
 * This service provides functionality to:
 * - Upload files as attachments to Confluence pages
 * - Add comments with download links to Confluence pages
 * - Handle authentication with Confluence instances
 *
 * It uses Retrofit for type-safe HTTP client operations and OkHttp for the underlying network stack.
 * The service is designed to be used as a Gradle BuildService for better resource management.
 */
abstract class ConfluenceService
    @Inject
    constructor() : BuildService<ConfluenceService.Params> {
        /**
         * Configuration parameters for the ConfluenceService.
         *
         * This interface defines the required configuration for initializing the service.
         * The parameters are provided through Gradle's configuration avoidance API.
         */
        interface Params : BuildServiceParameters {
            /**
             *  The base URL of the Confluence instance (e.g., "https://your-domain.atlassian.net/wiki/")
             */
            val baseUrl: Property<String>

            /**
             * The authentication credentials for the Confluence API, typically username and password
             */
            val credentials: Property<BasicAuthCredentials>
        }

        private val logger: Logger = Logging.getLogger("Confluence")

        internal abstract val okHttpClientProperty: Property<OkHttpClient>
        internal abstract val apiProperty: Property<ConfluenceApi>

        internal abstract val controllerProperty: Property<ConfluenceController>

        init {
            okHttpClientProperty.set(
                parameters.credentials.flatMap { it.username }
                    .zip(parameters.credentials.flatMap { it.password }) { username, password ->
                        ConfluenceClientFactory.build(username, password, logger)
                    },
            )
            apiProperty.set(
                okHttpClientProperty.zip(parameters.baseUrl) { client, baseUrl ->
                    ConfluenceApiFactory.build(client, baseUrl)
                },
            )
            controllerProperty.set(
                apiProperty.flatMap { api ->
                    parameters.baseUrl.map { baseUrl ->
                        ConfluenceControllerImpl(baseUrl, api)
                    }
                },
            )
        }

        private val controller: ConfluenceController get() = controllerProperty.get()

        /**
         * Uploads a file as an attachment to a Confluence page.
         *
         * This method handles the multipart form data upload process to the Confluence REST API.
         * It will throw an exception if the upload fails for any reason.
         *
         * @param pageId The ID of the Confluence page where the file should be attached
         * @param file The file to upload. Must be a valid, readable file.
         *
         * @throws IllegalStateException if the file doesn't exist or is not readable
         * @throws Exception if the API request fails or returns an error
         */
        fun uploadFile(
            pageId: String,
            file: File,
        ) {
            controller.uploadFile(pageId, file)
        }

        /**
         * Adds a comment with a file download link to a Confluence page.
         *
         * The comment will include a direct download link to the attached file.
         * The file must have been previously uploaded to the page.
         *
         * @param pageId The ID of the Confluence page where the comment should be added
         * @param fileName The name of the file to create a download link for
         *
         * @throws IllegalArgumentException if the pageId is empty or invalid
         * @throws Exception if the API request fails or returns an error
         */
        fun addComment(
            pageId: String,
            fileName: String,
        ) {
            controller.addComment(pageId, fileName)
        }
    }
