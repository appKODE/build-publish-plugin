package ru.kode.android.build.publish.plugin.slack.service.upload

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.util.UploadException
import ru.kode.android.build.publish.plugin.core.util.createPartFromString
import ru.kode.android.build.publish.plugin.core.util.executeOrThrow
import ru.kode.android.build.publish.plugin.slack.service.network.SlackNetworkService
import ru.kode.android.build.publish.plugin.slack.task.distribution.api.SlackUploadApi
import ru.kode.android.build.publish.plugin.slack.task.distribution.entity.UploadingFileRequest
import java.io.File
import javax.inject.Inject

private const val SLACK_BASE_URL = "https://slack.com/api/"

abstract class SlackUploadService
    @Inject
    constructor() : BuildService<SlackUploadService.Params> {
        interface Params : BuildServiceParameters {
            val uploadApiTokenFile: RegularFileProperty
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
                    .executeOrThrow()

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
                    .executeOrThrow()
                val filesAdapter = moshi.adapter<List<UploadingFileRequest>>()
                val files = filesAdapter.toJson(listOf(UploadingFileRequest(fileId, file.name)))
                uploadApi.completeUploading(
                    authorisation = getAuthorisationHeader(token),
                    files = files,
                    channels = channels.joinToString(),
                    initialComment = "$baseOutputFileName $buildName",
                ).executeOrThrow()
            } else {
                throw UploadException(requireNotNull(getUrlResponse.error))
            }
        }
    }

private fun getAuthorisationHeader(token: String): String {
    return "Bearer $token"
}
