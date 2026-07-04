package ru.kode.android.build.publish.plugin.confluence.service

import ru.kode.android.build.publish.plugin.confluence.controller.ConfluenceController
import ru.kode.android.build.publish.plugin.confluence.controller.factory.ConfluenceControllerFactory
import ru.kode.android.build.publish.plugin.core.api.service.BasicAuthBuildService
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
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
    constructor() : BasicAuthBuildService<ConfluenceController>() {
        override fun buildController(
            baseUrl: String,
            username: String,
            password: String,
            logger: PluginLogger,
        ): ConfluenceController =
            ConfluenceControllerFactory.build(
                baseUrl = baseUrl,
                username = username,
                password = password,
                logger = logger,
            )

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
