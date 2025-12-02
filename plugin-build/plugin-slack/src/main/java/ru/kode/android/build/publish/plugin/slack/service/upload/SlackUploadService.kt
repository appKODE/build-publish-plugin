package ru.kode.android.build.publish.plugin.slack.service.upload

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.util.UploadError
import ru.kode.android.build.publish.plugin.core.util.createPartFromString
import ru.kode.android.build.publish.plugin.core.util.executeWithResult
import ru.kode.android.build.publish.plugin.slack.service.network.SlackNetworkService
import ru.kode.android.build.publish.plugin.slack.task.distribution.api.SlackUploadApi
import ru.kode.android.build.publish.plugin.slack.task.distribution.entity.UploadingFileRequest
import java.io.File
import javax.inject.Inject

private const val SLACK_BASE_URL = "https://slack.com/api/"

/**
 * A service that handles file uploads to Slack using the Slack Files API.
 *
 * This service provides functionality to:
 * - Obtain upload URLs from Slack
 * - Upload files to Slack
 * - Complete the upload process and share files in channels
 * - Handle authentication and error cases
 *
 * It's designed to work with the Slack API's file upload flow, which involves
 * multiple steps to support large file uploads.
 */
abstract class SlackUploadService
    @Inject
    constructor() : BuildService<SlackUploadService.Params> {
        /**
         * Configuration parameters for the SlackUploadService.
         */
        interface Params : BuildServiceParameters {
            /**
             * File containing the Slack API token for file uploads
             */
            val uploadApiTokenFile: RegularFileProperty

            /**
             * The network service to use for HTTP operations
             */
            val networkService: Property<SlackNetworkService>
        }

        internal abstract val uploadApiProperty: Property<SlackUploadApi>

        init {
            uploadApiProperty.set(
                parameters.networkService.flatMap { it.retrofitProperty }.map { retrofit ->
                    retrofit.baseUrl(SLACK_BASE_URL).build()
                        .create(SlackUploadApi::class.java)
                },
            )
        }

        private val moshi: Moshi get() = parameters.networkService.flatMap { it.moshiProperty }.get()
        private val uploadApi: SlackUploadApi get() = uploadApiProperty.get()
        private val token: String get() = parameters.uploadApiTokenFile.get().asFile.readText()

        /**
         * Uploads a file to Slack and shares it in the specified channels.
         *
         * This method performs a multi-step upload process:
         * 1. Requests an upload URL from Slack
         * 2. Uploads the file content to the provided URL
         * 3. Completes the upload and shares the file in the specified channels
         *
         * @param baseOutputFileName The base name to use for the uploaded file
         * @param buildName The build name or identifier to include in the comment
         * @param file The file to upload
         * @param channels Set of channel IDs or names where the file should be shared
         *
         * @throws UploadError if any step of the upload process fails
         */
        fun upload(
            baseOutputFileName: String,
            buildName: String,
            file: File,
            channels: Set<String>,
        ) {
            val getUrlResponse =
                uploadApi
                    .getUploadUrl(
                        authorisation = getAuthorisationHeader(token),
                        fileName = file.name,
                        length = file.length(),
                    )
                    .executeWithResult()
                    .getOrThrow()

            if (getUrlResponse.ok) {
                val url = requireNotNull(getUrlResponse.uploadUrl)
                val fileId = requireNotNull(getUrlResponse.fileId)
                val filePart =
                    MultipartBody.Part.createFormData(
                        "file",
                        file.name,
                        file.asRequestBody(),
                    )
                uploadApi
                    .upload(
                        authorisation = getAuthorisationHeader(token),
                        url = url,
                        fileName = createPartFromString(file.name),
                        filePart = filePart,
                    )
                    .executeWithResult()
                    .getOrThrow()
                val filesAdapter = moshi.adapter<List<UploadingFileRequest>>()
                val files = filesAdapter.toJson(listOf(UploadingFileRequest(fileId, file.name)))
                uploadApi
                    .completeUploading(
                        authorisation = getAuthorisationHeader(token),
                        files = files,
                        channels = channels.joinToString(),
                        initialComment = "$baseOutputFileName $buildName",
                    )
                    .executeWithResult()
                    .getOrThrow()
            } else {
                throw UploadError(requireNotNull(getUrlResponse.error))
            }
        }
    }

private fun getAuthorisationHeader(token: String): String {
    return "Bearer $token"
}
