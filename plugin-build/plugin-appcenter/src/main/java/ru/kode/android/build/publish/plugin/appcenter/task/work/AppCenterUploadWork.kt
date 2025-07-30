package ru.kode.android.build.publish.plugin.appcenter.task.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.appcenter.core.MAX_REQUEST_COUNT
import ru.kode.android.build.publish.plugin.appcenter.core.MAX_REQUEST_DELAY_MS
import ru.kode.android.build.publish.plugin.appcenter.task.entity.ChunkRequestBody
import ru.kode.android.build.publish.plugin.appcenter.task.uploader.AppCenterUploader
import ru.kode.android.build.publish.plugin.core.util.ellipsizeAt
import kotlin.math.round

interface AppCenterUploadParameters : WorkParameters {
    val ownerName: Property<String>
    val appName: Property<String>
    val apiToken: Property<String>
    val buildName: Property<String>
    val buildNumber: Property<String>
    val outputFile: RegularFileProperty
    val testerGroups: SetProperty<String>
    val changelogFile: RegularFileProperty
    val maxUploadStatusRequestCount: Property<Int>
    val uploadStatusRequestDelayMs: Property<Long>
    val uploadStatusRequestDelayCoefficient: Property<Long>
}

abstract class AppCenterUploadWork : WorkAction<AppCenterUploadParameters> {
    private val logger = Logging.getLogger(this::class.java)

    override fun execute() {
        val uploader =
            AppCenterUploader(
                parameters.ownerName.get(),
                parameters.appName.get(),
                logger,
                parameters.apiToken.get(),
            )

        val outputFile = parameters.outputFile.asFile.get()
        val testerGroups = parameters.testerGroups.get()
        val changelogFile = parameters.changelogFile.asFile.get()

        logger.info("Step 1/7: Prepare upload")
        val prepareResponse =
            uploader.prepareRelease(
                buildVersion = parameters.buildName.get(),
                buildNumber = parameters.buildNumber.get(),
            )
        uploader.initUploadApi(prepareResponse.upload_domain ?: "https://file.appcenter.ms")

        logger.info("Step 2/7: Send metadata")
        val packageAssetId = prepareResponse.package_asset_id
        val encodedToken = prepareResponse.url_encoded_token
        val metaResponse = uploader.sendMetaData(outputFile, packageAssetId, encodedToken)

        // See NOTE_CHUNKS_UPLOAD_LOOP
        logger.info("Step 3/7: Upload apk file chunks")
        metaResponse.chunk_list.forEachIndexed { i, chunkNumber ->
            val range = (i * metaResponse.chunk_size)..((i + 1) * metaResponse.chunk_size)
            logger.info("Step 3/7 : Upload chunk ${i + 1}/${metaResponse.chunk_list.size}")
            uploader.uploadChunk(
                packageAssetId = packageAssetId,
                encodedToken = encodedToken,
                chunkNumber = chunkNumber,
                request = ChunkRequestBody(outputFile, range, "application/octet-stream"),
            )
        }

        logger.info("Step 4/7: Finish upload")
        uploader.sendUploadIsFinished(packageAssetId, encodedToken)

        logger.info("Step 5/7: Commit uploaded release")
        uploader.commit(prepareResponse.id)

        logger.info("Step 6/7: Fetching for release to be ready to publish")
        val publishResponse =
            uploader
                .waitingReadyToBePublished(
                    preparedUploadId = prepareResponse.id,
                    maxRequestCount =
                        parameters.maxUploadStatusRequestCount.orNull
                            ?: MAX_REQUEST_COUNT,
                    requestDelayMs = requestDelayMs(parameters, outputFile.length()),
                )
        logger.info("Step 7/7: Distribute to the app testers: $testerGroups")
        val releaseId = publishResponse.release_distinct_id
        if (releaseId != null) {
            uploader.distribute(
                releaseId = releaseId,
                distributionGroups = testerGroups,
                releaseNotes = changelogFile.readText().ellipsizeAt(MAX_NOTES_CHARACTERS_COUNT),
            )
        } else {
            logger.error(
                "Apk was uploaded, " +
                    "but distributors will not be notified: " +
                    "field 'release_distinct_id' is null, cannot execute 'distribute' request",
            )
        }
        logger.info("upload done")
    }

    private fun requestDelayMs(
        params: AppCenterUploadParameters,
        fileSizeBytes: Long,
    ): Long {
        val coefficient = params.uploadStatusRequestDelayCoefficient.orNull
        return if (coefficient != null) {
            round((fileSizeBytes.bytesToMegabytes() / coefficient) * MILLIS_IN_SEC).toLong()
        } else {
            parameters.uploadStatusRequestDelayMs.orNull ?: MAX_REQUEST_DELAY_MS
        }
    }
}

@Suppress("MagicNumber") // well known formula
private fun Long.bytesToMegabytes(): Double {
    return this / BYTES_PER_MEGABYTE
}

private const val BYTES_PER_MEGABYTE = 1024.0 * 1024.0
private const val MILLIS_IN_SEC = 1000
private const val MAX_NOTES_CHARACTERS_COUNT = 5000
