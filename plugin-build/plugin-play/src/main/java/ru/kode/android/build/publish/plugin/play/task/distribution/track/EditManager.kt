package ru.kode.android.build.publish.plugin.play.task.distribution.track

import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.ReleaseStatus
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.ResolutionStrategy
import java.io.File

internal interface EditManager {
    fun promoteRelease(
        promoteTrackName: String,
        fromTrackName: String,
        releaseStatus: ReleaseStatus?,
        releaseName: String?,
        releaseNotes: Map<String, String?>?,
        userFraction: Double?,
        updatePriority: Int?,
        retainableArtifacts: List<Long>?,
        versionCode: Long?,
    )

    fun uploadBundle(
        bundleFile: File,
        strategy: ResolutionStrategy,
    ): Long?

    fun publishArtifacts(
        versionCodes: List<Long>,
        didPreviousBuildSkipCommit: Boolean,
        trackName: String,
        releaseStatus: ReleaseStatus?,
        releaseName: String?,
        releaseNotes: Map<String, String?>?,
        userFraction: Double?,
        updatePriority: Int?,
        retainableArtifacts: List<Long>?,
    )
}
