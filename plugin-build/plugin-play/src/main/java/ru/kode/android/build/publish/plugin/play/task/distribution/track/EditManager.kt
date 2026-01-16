package ru.kode.android.build.publish.plugin.play.task.distribution.track

import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.ReleaseStatus
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.ResolutionStrategy
import java.io.File

/**
 * Manages a Play Console edit lifecycle.
 *
 * An edit encapsulates a set of changes (bundle uploads, track updates, etc.) that are applied
 * to Play Console when the edit is committed.
 */
internal interface EditManager {
    /**
     * Promotes a release from one track to another within the current edit.
     */
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

    /**
     * Uploads the given App Bundle file into the current edit.
     *
     * @return The uploaded bundle's version code, or `null` when upload was skipped/ignored.
     */
    fun uploadBundle(
        bundleFile: File,
        strategy: ResolutionStrategy,
    ): Long?

    /**
     * Publishes the given artifacts to the specified track.
     */
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
