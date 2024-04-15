package ru.kode.android.build.publish.plugin.task.play.track

import ru.kode.android.build.publish.plugin.task.play.publisher.ReleaseStatus
import ru.kode.android.build.publish.plugin.task.play.publisher.ResolutionStrategy
import java.io.File

interface EditManager {
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
