package ru.kode.android.build.publish.plugin.play.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.play.service.network.PlayNetworkService

internal interface PlayUploadParameters : WorkParameters {
    val outputFile: RegularFileProperty
    val trackId: Property<String>
    val releaseName: Property<String>
    val updatePriority: Property<Int>
    val networkService: Property<PlayNetworkService>
}

internal abstract class PlayUploadWork : WorkAction<PlayUploadParameters> {
    override fun execute() {
        val track = parameters.trackId.get()
        val priority = parameters.updatePriority.orNull ?: 0
        val releaseName = parameters.releaseName.get()
        val file = parameters.outputFile.asFile.get()

        val service = parameters.networkService.get()
        service.upload(
            file = file,
            trackId = track,
            priority = priority,
            releaseName = releaseName,
        )
    }
}
