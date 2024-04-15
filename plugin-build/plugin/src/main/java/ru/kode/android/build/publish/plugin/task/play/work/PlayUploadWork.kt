package ru.kode.android.build.publish.plugin.task.play.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.task.play.publisher.DefaultPlayPublisher
import ru.kode.android.build.publish.plugin.task.play.publisher.EditResponse
import ru.kode.android.build.publish.plugin.task.play.publisher.ResolutionStrategy
import ru.kode.android.build.publish.plugin.task.play.publisher.createPublisher
import ru.kode.android.build.publish.plugin.task.play.track.DefaultEditManager
import ru.kode.android.build.publish.plugin.task.play.track.DefaultTrackManager
import ru.kode.android.build.publish.plugin.task.play.track.TrackManager

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

        val publisher = DefaultPlayPublisher(
            publisher = createPublisher(parameters.apiToken.asFile.get().inputStream()),
            appId = parameters.appId.get()
        )
        logger.info("Step 1/3: Requesting track edit...")
        val editId = when (val result = publisher.insertEdit()) {
            is EditResponse.Success -> result.id
            is EditResponse.Failure -> null
        }

        if (editId == null) {
            logger.error("Failed to fetch edit id to upload bundle. Check your credentials")
            return
        }

        val trackManager = DefaultTrackManager(publisher, editId)
        val editManager = DefaultEditManager(publisher, trackManager, editId)

        logger.info("Step 2/3: Upload bundle for $editId")

        val versionCode = editManager.uploadBundle(file, ResolutionStrategy.IGNORE)

        if (versionCode == null) {
            logger.error("Failed to upload bundle. Check your credentials")
            return
        }

        logger.info("Step 3/3: Pushing $releaseName to $track at P=$priority V=$versionCode")

        trackManager.update(
            config = TrackManager.UpdateConfig(
                trackName = track,
                versionCodes = listOf(versionCode),
                didPreviousBuildSkipCommit = false,
                TrackManager.BaseConfig(
                    userFraction = parameters.versionCode.orNull ?: 0.1,
                    updatePriority = priority,
                    releaseName = parameters.releaseName.get()
                )
            )
        )
        logger.info("Step 3/3: Bundle upload successful")
    }

}
