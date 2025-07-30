package ru.kode.android.build.publish.plugin.play.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.DefaultPlayPublisher
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.EditResponse
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.ResolutionStrategy
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.createPublisher
import ru.kode.android.build.publish.plugin.play.task.distribution.track.DefaultEditManager
import ru.kode.android.build.publish.plugin.play.task.distribution.track.DefaultTrackManager
import ru.kode.android.build.publish.plugin.play.task.distribution.track.TrackManager

interface PlayUploadParameters : WorkParameters {
    val appId: Property<String>
    val apiToken: RegularFileProperty
    val outputFile: RegularFileProperty
    val trackId: Property<String>
    val releaseName: Property<String>
    val versionCode: Property<Double>
    val updatePriority: Property<Int>
}

abstract class PlayUploadWork : WorkAction<PlayUploadParameters> {
    private val logger = Logging.getLogger(this::class.java)

    override fun execute() {
        val track = parameters.trackId.get()
        val priority = parameters.updatePriority.orNull ?: 0
        val releaseName = parameters.releaseName.get()
        val file = parameters.outputFile.asFile.get()

        val publisher =
            DefaultPlayPublisher(
                publisher = createPublisher(parameters.apiToken.asFile.get().inputStream()),
                appId = parameters.appId.get(),
            )
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

        logger.info("Step 3/4: Pushing $releaseName to $track at P=$priority V=$versionCode")

        trackManager.update(
            config =
                TrackManager.UpdateConfig(
                    trackName = track,
                    versionCodes = listOf(versionCode),
                    didPreviousBuildSkipCommit = false,
                    TrackManager.BaseConfig(
                        updatePriority = priority,
                        releaseName = parameters.releaseName.get(),
                    ),
                ),
        )

        logger.info("Step 3/4: Commit $editId")

        publisher.commitEdit(editId)

        logger.info("Step 4/4: Bundle upload successful")
    }
}
