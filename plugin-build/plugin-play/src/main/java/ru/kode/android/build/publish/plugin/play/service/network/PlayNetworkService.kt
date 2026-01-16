package ru.kode.android.build.publish.plugin.play.service.network

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.DefaultPlayPublisher
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.EditResponse
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.InternalPlayPublisher
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.ResolutionStrategy
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.createPublisher
import ru.kode.android.build.publish.plugin.play.task.distribution.track.DefaultEditManager
import ru.kode.android.build.publish.plugin.play.task.distribution.track.DefaultTrackManager
import ru.kode.android.build.publish.plugin.play.task.distribution.track.TrackManager
import java.io.File
import javax.inject.Inject

/**
 * A network service for interacting with the Google Play Developer API.
 *
 * This service provides functionality to:
 * - Create and manage app edits on the Play Store
 * - Upload app bundles to the Play Store
 * - Manage release tracks and versions
 * - Handle the complete app publishing workflow
 *
 * It uses the Google Play Developer API to perform these operations and is designed
 * to be used as a Gradle BuildService for better resource management.
 */
abstract class PlayNetworkService
    @Inject
    constructor() : BuildService<PlayNetworkService.Params> {
        /**
         * Configuration parameters for the PlayNetworkService.
         */
        interface Params : BuildServiceParameters {
            /**
             * The file containing the Google Play API service account credentials
             */
            val apiTokenFile: RegularFileProperty

            /**
             * The application ID (package name) in the Play Store
             */
            val appId: Property<String>

            /**
             * The service for logging messages.
             */
            val loggerService: Property<LoggerService>
        }

        /**
         * Lazily-created Play publisher client used to interact with the Google Play Developer API.
         */
        internal abstract val publisherProperty: Property<InternalPlayPublisher>

        init {
            publisherProperty.set(
                parameters.apiTokenFile.zip(parameters.appId) { token, appId ->
                    DefaultPlayPublisher(
                        publisher = createPublisher(token.asFile.inputStream()),
                        appId = appId,
                    )
                },
            )
        }

        private val logger = parameters.loggerService.get()

        private val publisher: InternalPlayPublisher get() = publisherProperty.get()

        /**
         * Uploads an app bundle to the specified track in the Play Store.
         *
         * This method performs the complete upload process:
         * 1. Creates a new edit on the Play Store
         * 2. Uploads the bundle file
         * 3. Updates the specified track with the new version
         * 4. Commits the edit to make changes live
         *
         * @param file The app bundle file to upload
         * @param trackId The track to publish to (e.g., 'internal', 'alpha', 'beta', 'production')
         * @param releaseName The name of this release (visible in Play Console)
         * @param priority The in-app update priority (0..5)
         *
         * @throws IllegalStateException if the upload process fails at any step
         */
        fun upload(
            file: File,
            trackId: String,
            releaseName: String,
            priority: Int,
        ) {
            logger.info("Step 1/4: Requesting track edit...")

            val editId =
                when (val result = publisher.insertEdit()) {
                    is EditResponse.Success -> result.id
                    is EditResponse.Failure -> {
                        if (result.isNewApp()) {
                            logger.error("Error response: app does not exist")
                        } else {
                            logger.error("Error response: $result")
                        }
                        null
                    }
                }

            if (editId == null) {
                logger.error("Failed to fetch edit id to upload bundle")
                return
            }

            val trackManager = DefaultTrackManager(publisher, editId)
            val editManager = DefaultEditManager(publisher, trackManager, editId)

            logger.info("Step 2/4: Upload bundle for $editId")

            val versionCode = editManager.uploadBundle(file, ResolutionStrategy.IGNORE)

            if (versionCode == null) {
                logger.error("Failed to upload bundle")
                return
            }

            logger.info("Step 3/4: Pushing $releaseName to $trackId at P=$priority V=$versionCode")

            trackManager.update(
                config =
                    TrackManager.UpdateConfig(
                        trackName = trackId,
                        versionCodes = listOf(versionCode),
                        didPreviousBuildSkipCommit = false,
                        TrackManager.BaseConfig(
                            updatePriority = priority,
                            releaseName = releaseName,
                        ),
                    ),
            )

            logger.info("Step 3/4: Commit $editId")

            publisher.commitEdit(editId)

            logger.info("Step 4/4: Bundle upload successful")
        }
    }
