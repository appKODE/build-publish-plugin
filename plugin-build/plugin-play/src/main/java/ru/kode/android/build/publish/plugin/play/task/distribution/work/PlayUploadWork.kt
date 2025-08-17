package ru.kode.android.build.publish.plugin.play.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.play.service.network.PlayNetworkService

/**
 * Parameters for the Play Store upload work action.
 *
 * This interface defines the input parameters required for the [PlayUploadWork] action.
 */
internal interface PlayUploadParameters : WorkParameters {
    /**
     * The Android App Bundle (.aab) file to upload
     */
    val distributionFile: RegularFileProperty

    /**
     * The target track for the release (e.g., 'internal', 'alpha', 'beta', 'production')
     */
    val trackId: Property<String>

    /**
     * The name of the release to display in the Play Console
     */
    val releaseName: Property<String>

    /**
     * The update priority for the release (0-5)
     */
    val updatePriority: Property<Int>

    /**
     * The Play Store network service to use for the upload
     */
    val networkService: Property<PlayNetworkService>
}

/**
 * A Gradle work action that handles uploading an app bundle to the Google Play Store.
 *
 * This work action is responsible for:
 * - Retrieving upload parameters
 * - Validating the input file
 * - Delegating the upload to the PlayNetworkService
 * - Handling any errors that occur during the upload
 *
 * The actual upload is performed asynchronously by Gradle's worker API.
 */
internal abstract class PlayUploadWork : WorkAction<PlayUploadParameters> {
    override fun execute() {
        val track = parameters.trackId.get()
        val priority = parameters.updatePriority.orNull ?: 0
        val releaseName = parameters.releaseName.get()
        val file = parameters.distributionFile.asFile.get()

        val service = parameters.networkService.get()
        service.upload(
            file = file,
            trackId = track,
            priority = priority,
            releaseName = releaseName,
        )
    }
}
