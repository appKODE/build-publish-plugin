package ru.kode.android.build.publish.plugin.play.service

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
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

abstract class PlayNetworkService @Inject constructor() : BuildService<PlayNetworkService.Params> {

    interface Params : BuildServiceParameters {
        val apiTokenFile: RegularFileProperty
        val appId: Property<String>
    }

    internal abstract val publisherProperty: Property<InternalPlayPublisher>

    init {
        publisherProperty.set(
            parameters.apiTokenFile.zip(parameters.appId) { token, appId ->
                DefaultPlayPublisher(
                    publisher = createPublisher(token.asFile.inputStream()),
                    appId = parameters.appId.get(),
                )
            }
        )
    }

    private val publisher: InternalPlayPublisher get() = publisherProperty.get()

    fun upload(
        file: File,
        trackId: String,
        releaseName: String,
        priority: Int
    ) {
        logger.info("Step 1/4: Requesting track edit...")

        val editId = when (val result = publisher.insertEdit()) {
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

    companion object {
        private val logger: Logger = Logging.getLogger(PlayNetworkService::class.java)
    }
}
