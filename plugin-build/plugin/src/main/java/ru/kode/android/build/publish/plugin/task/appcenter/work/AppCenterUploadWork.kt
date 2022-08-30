package ru.kode.android.build.publish.plugin.task.appcenter.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.task.appcenter.entity.ChunkRequestBody
import ru.kode.android.build.publish.plugin.task.appcenter.uploader.AppCenterUploader

interface AppCenterUploadParameters : WorkParameters {
    val ownerName: Property<String>
    val appName: Property<String>
    val apiToken: Property<String>
    val buildName: Property<String>
    val buildNumber: Property<String>
    val outputFile: RegularFileProperty
    val testerGroups: SetProperty<String>
    val changelogFile: RegularFileProperty
    val maxRequestCount: Property<Int>
    val requestDelayMs: Property<Long>
}

abstract class AppCenterUploadWork : WorkAction<AppCenterUploadParameters> {

    private val logger = Logging.getLogger(this::class.java)

    override fun execute() {
        val uploader = AppCenterUploader(
            parameters.ownerName.get(),
            parameters.appName.get(),
            logger,
            parameters.apiToken.get()
        )

        val outputFile = parameters.outputFile.asFile.get()
        val testerGroups = parameters.testerGroups.get()
        val changelogFile = parameters.changelogFile.asFile.get()

        logger.debug("Step 1/7: Prepare upload")
        val prepareResponse = uploader.prepareRelease(
            buildVersion = parameters.buildName.get(),
            buildNumber = parameters.buildNumber.get()
        )
        uploader.initUploadApi(prepareResponse.upload_domain ?: "https://file.appcenter.ms")

        logger.debug("Step 2/7: Send metadata")
        val packageAssetId = prepareResponse.package_asset_id
        val encodedToken = prepareResponse.url_encoded_token
        val metaResponse = uploader.sendMetaData(outputFile, packageAssetId, encodedToken)

        // See NOTE_CHUNKS_UPLOAD_LOOP
        logger.debug("Step 3/7: Upload apk file chunks")
        metaResponse.chunk_list.forEachIndexed { i, chunkNumber ->
            val range = (i * metaResponse.chunk_size)..((i + 1) * metaResponse.chunk_size)
            logger.debug("Step 3/7 : Upload chunk ${i + 1}/${metaResponse.chunk_list.size}")
            uploader.uploadChunk(
                packageAssetId = packageAssetId,
                encodedToken = encodedToken,
                chunkNumber = chunkNumber,
                request = ChunkRequestBody(outputFile, range, "application/octet-stream")
            )
        }

        logger.debug("Step 4/7: Finish upload")
        uploader.sendUploadIsFinished(packageAssetId, encodedToken)

        logger.debug("Step 5/7: Commit uploaded release")
        uploader.commit(prepareResponse.id)

        logger.debug("Step 6/7: Fetching for release to be ready to publish")
        val publishResponse = uploader
            .waitingReadyToBePublished(
                preparedUploadId = prepareResponse.id,
                maxRequestCount = parameters.maxRequestCount.orNull ?: MAX_REQUEST_COUNT,
                requestDelayMs = parameters.requestDelayMs.orNull ?: MAX_REQUEST_DELAY_MS
            )
        logger.debug("Step 7/7: Distribute to the app testers: $testerGroups")
        val releaseId = publishResponse.release_distinct_id
        if (releaseId != null) {
            uploader.distribute(releaseId, testerGroups, changelogFile.readText())
        } else {
            logger.error(
                "Apk was uploaded, " +
                    "but distributors will not be notified: " +
                    "field 'release_distinct_id' is null, cannot execute 'distribute' request"
            )
        }
        logger.debug("upload done")
    }
}

private const val MAX_REQUEST_COUNT = 20
private const val MAX_REQUEST_DELAY_MS = 2000L
