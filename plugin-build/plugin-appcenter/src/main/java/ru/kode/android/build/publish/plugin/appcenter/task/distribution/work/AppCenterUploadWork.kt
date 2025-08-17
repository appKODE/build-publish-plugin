package ru.kode.android.build.publish.plugin.appcenter.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.appcenter.config.MAX_REQUEST_COUNT
import ru.kode.android.build.publish.plugin.appcenter.config.MAX_REQUEST_DELAY_MS
import ru.kode.android.build.publish.plugin.appcenter.service.network.AppCenterNetworkService
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.entity.ChunkRequestBody
import ru.kode.android.build.publish.plugin.core.util.ellipsizeAt
import kotlin.math.round

private const val BYTES_PER_MEGABYTE = 1024.0 * 1024.0
private const val MILLIS_IN_SEC = 1000
private const val MAX_NOTES_CHARACTERS_COUNT = 5000

/**
 * Configuration parameters for the AppCenter upload work action.
 *
 * This interface defines all the inputs required to perform an AppCenter upload,
 * including file references, build information, and network service configuration.
 */
internal interface AppCenterUploadParameters : WorkParameters {
    /**
     * Name of the application in AppCenter
     */
    val appName: Property<String>

    /**
     * Version name of the build
     */
    val buildName: Property<String>

    /**
     * Build number or version code
     */
    val buildNumber: Property<String>

    /**
     * The APK file to be uploaded
     */
    val outputFile: RegularFileProperty

    /**
     * Set of distribution group names to receive the build
     */
    val testerGroups: SetProperty<String>

    /**
     * File containing release notes for the distribution
     */
    val changelogFile: RegularFileProperty

    /**
     * Maximum number of polling attempts for upload status
     */
    val maxUploadStatusRequestCount: Property<Int>

    /**
     * Fixed delay between status polling requests
     */
    val uploadStatusRequestDelayMs: Property<Long>

    /**
     * Dynamic delay coefficient based on file size
     */
    val uploadStatusRequestDelayCoefficient: Property<Long>

    /**
     * The network service for AppCenter API communication
     */
    val networkService: Property<AppCenterNetworkService>
}

/**
 * Gradle WorkAction that orchestrates the upload of an APK to AppCenter.
 *
 * This class implements the complete upload workflow to AppCenter's distribution service,
 * handling all the necessary API calls and error conditions. It's designed to run asynchronously
 * as part of Gradle's worker API.
 *
 * ## Error Handling
 * - Network timeouts and retries are handled by the underlying [AppCenterNetworkService]
 * - Failed chunk uploads will be automatically retried
 * - The process fails fast on critical errors (e.g., authentication failures)
 *
 * ## Performance Considerations
 * - Uses chunked uploads for better reliability with large APKs
 * - Implements exponential backoff for status polling
 * - Logs detailed progress information at each step
 *
 * @see AppCenterNetworkService For the underlying API communication
 * @see AppCenterDistributionTask For the Gradle task that creates this work
 */
internal abstract class AppCenterUploadWork : WorkAction<AppCenterUploadParameters> {
    private val logger = Logging.getLogger(this::class.java)

    override fun execute() {
        val networkService = parameters.networkService.get()

        val outputFile = parameters.outputFile.asFile.get()
        val testerGroups = parameters.testerGroups.get()
        val changelogFile = parameters.changelogFile.asFile.get()

        logger.info("Step 1/7: Prepare upload")
        val prepareResponse =
            networkService.prepareRelease(
                buildVersion = parameters.buildName.get(),
                buildNumber = parameters.buildNumber.get(),
            )
        networkService.initUploadApi(prepareResponse.upload_domain ?: "https://file.appcenter.ms")
        networkService.initAppName(parameters.appName.get())

        logger.info("Step 2/7: Send metadata")
        val packageAssetId = prepareResponse.package_asset_id
        val encodedToken = prepareResponse.url_encoded_token
        val metaResponse = networkService.sendMetaData(outputFile, packageAssetId, encodedToken)

        // See NOTE_CHUNKS_UPLOAD_LOOP
        logger.info("Step 3/7: Upload apk file chunks")
        metaResponse.chunk_list.forEachIndexed { i, chunkNumber ->
            val range = (i * metaResponse.chunk_size)..((i + 1) * metaResponse.chunk_size)
            logger.info("Step 3/7 : Upload chunk ${i + 1}/${metaResponse.chunk_list.size}")
            networkService.uploadChunk(
                packageAssetId = packageAssetId,
                encodedToken = encodedToken,
                chunkNumber = chunkNumber,
                request = ChunkRequestBody(outputFile, range, "application/octet-stream"),
            )
        }

        logger.info("Step 4/7: Finish upload")
        networkService.sendUploadIsFinished(packageAssetId, encodedToken)

        logger.info("Step 5/7: Commit uploaded release")
        networkService.commit(prepareResponse.id)

        logger.info("Step 6/7: Fetching for release to be ready to publish")
        val publishResponse =
            networkService
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
            networkService.distribute(
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

    /**
     * Calculates the delay between upload status polling requests.
     *
     * The delay can be calculated in two ways:
     * 1. Dynamically based on file size and a coefficient (if coefficient is provided)
     * 2. Using a fixed delay (if coefficient is not provided)
     *
     * @param params The upload parameters containing delay configuration
     * @param fileSizeBytes Size of the file being uploaded in bytes
     *
     * @return The calculated delay in milliseconds
     */
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
