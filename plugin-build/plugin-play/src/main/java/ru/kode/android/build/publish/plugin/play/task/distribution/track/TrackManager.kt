package ru.kode.android.build.publish.plugin.play.task.distribution.track

import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.InternalPlayPublisher
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.ReleaseStatus

/**
 * Manages Play Console release tracks within an existing edit.
 *
 * Implementations are responsible for creating/updating [Track] payloads (including releases)
 * and delegating API calls to an [InternalPlayPublisher].
 */
internal interface TrackManager {
    /**
     * Updates (publishes) the given set of artifacts to a track.
     */
    fun update(config: UpdateConfig)

    /**
     * Promotes a release from one track to another.
     */
    fun promote(config: PromoteConfig)

    /**
     * Base configuration shared between update/promote operations.
     */
    data class BaseConfig(
        val releaseStatus: ReleaseStatus? = null,
        val userFraction: Double? = null,
        val updatePriority: Int?,
        val releaseNotes: Map<String, String?>? = emptyMap(),
        val retainableArtifacts: List<Long>? = null,
        val releaseName: String?,
    )

    /**
     * Parameters for updating a track with a new release.
     */
    data class UpdateConfig(
        val trackName: String,
        val versionCodes: List<Long>,
        val didPreviousBuildSkipCommit: Boolean,
        val base: BaseConfig,
    )

    /**
     * Parameters for promoting an existing release between tracks.
     */
    data class PromoteConfig(
        val promoteTrackName: String,
        val fromTrackName: String,
        val versionCode: Long?,
        val base: BaseConfig,
    )
}

/**
 * Default [TrackManager] implementation backed by [InternalPlayPublisher].
 */
internal class DefaultTrackManager(
    private val publisher: InternalPlayPublisher,
    private val editId: String,
) : TrackManager {
    override fun update(config: TrackManager.UpdateConfig) {
        val track =
            if (config.didPreviousBuildSkipCommit) {
                createTrackForSkippedCommit(config)
            } else if (config.base.releaseStatus.orDefault().isRollout()) {
                createTrackForRollout(config)
            } else {
                createDefaultTrack(config)
            }

        publisher.updateTrack(editId, track)
    }

    override fun promote(config: TrackManager.PromoteConfig) {
        val track = publisher.getTrack(editId, config.fromTrackName)
        check(track.releases.orEmpty().flatMap { it.versionCodes.orEmpty() }.isNotEmpty()) {
            "Track '${config.fromTrackName}' has no releases. Did you mean to run publish?"
        }

        // Update the track
        for (release in track.releases) {
            release.mergeChanges(config.versionCode?.let { listOf(it) }, config.base)
        }
        // Only keep the unique statuses from the highest version code since duplicate statuses are
        // not allowed. This is how we deal with an update from inProgress -> completed. We update
        // all the tracks to completed, then get rid of the one that used to be inProgress.
        track.releases =
            track.releases.sortedByDescending {
                it.versionCodes?.maxOrNull()
            }.distinctBy {
                it.status
            }

        println("Promoting release from track '${track.track}'")
        track.track = config.promoteTrackName
        publisher.updateTrack(editId, track)
    }

    @Suppress("NestedBlockDepth") // TODO refactor and simplify TrackManager
    private fun createTrackForSkippedCommit(config: TrackManager.UpdateConfig): Track {
        val track = publisher.getTrack(editId, config.trackName)

        if (track.releases.isNullOrEmpty()) {
            track.releases = listOf(TrackRelease().mergeChanges(config.versionCodes, config.base))
        } else {
            val hasReleaseToBeUpdated =
                track.releases.firstOrNull {
                    it.status == config.base.releaseStatus.orDefault().publishedName
                } != null

            if (hasReleaseToBeUpdated) {
                for (release in track.releases) {
                    if (release.status == config.base.releaseStatus.orDefault().publishedName) {
                        release.mergeChanges(
                            release.versionCodes.orEmpty() + config.versionCodes,
                            config.base,
                        )
                    }
                }
            } else {
                val release =
                    TrackRelease().mergeChanges(config.versionCodes, config.base).apply {
                        maybeCopyChangelogFromPreviousRelease(config.trackName)
                    }
                track.releases = track.releases + release
            }
        }

        return track
    }

    private fun createTrackForRollout(config: TrackManager.UpdateConfig): Track {
        val track = publisher.getTrack(editId, config.trackName)

        val keep = track.releases.orEmpty().filterNot { it.isRollout() }
        val release =
            TrackRelease().mergeChanges(config.versionCodes, config.base).apply {
                maybeCopyChangelogFromPreviousRelease(config.trackName)
            }
        track.releases = keep + release

        return track
    }

    private fun createDefaultTrack(config: TrackManager.UpdateConfig) =
        Track().apply {
            track = config.trackName
            val release =
                TrackRelease()
                    .mergeChanges(config.versionCodes, config.base)
                    .apply { maybeCopyChangelogFromPreviousRelease(config.trackName) }
            releases = listOf(release)
        }

    private fun TrackRelease.maybeCopyChangelogFromPreviousRelease(trackName: String) {
        if (!releaseNotes.isNullOrEmpty()) return

        val previousRelease =
            publisher.getTrack(editId, trackName)
                .releases
                .orEmpty()
                .maxByOrNull { it.versionCodes.orEmpty().maxOrNull() ?: 1 }
        releaseNotes = previousRelease?.releaseNotes
    }

    private fun TrackRelease.mergeChanges(
        versionCodes: List<Long>?,
        config: TrackManager.BaseConfig,
    ) = apply {
        updateVersionCodes(versionCodes, config.retainableArtifacts)
        updateStatus(config.releaseStatus, config.userFraction != null)
        updateConsoleName(config.releaseName)
        updateReleaseNotes(config.releaseNotes)
        updateUserFraction(config.userFraction)
        updateUpdatePriority(config.updatePriority)
    }

    private fun TrackRelease.updateVersionCodes(
        versionCodes: List<Long>?,
        retainableArtifacts: List<Long>?,
    ) {
        val newVersions = versionCodes ?: this.versionCodes.orEmpty()
        this.versionCodes = newVersions + retainableArtifacts.orEmpty()
    }

    private fun TrackRelease.updateStatus(
        releaseStatus: ReleaseStatus?,
        hasUserFraction: Boolean,
    ) {
        if (releaseStatus != null) {
            status = releaseStatus.publishedName
        } else if (hasUserFraction) {
            status = ReleaseStatus.IN_PROGRESS.publishedName
        } else if (status == null) {
            status = DEFAULT_RELEASE_STATUS.publishedName
        }
    }

    private fun TrackRelease.updateConsoleName(releaseName: String?) {
        if (releaseName != null) name = releaseName
    }

    private fun TrackRelease.updateReleaseNotes(rawReleaseNotes: Map<String, String?>?) {
        val releaseNotes =
            rawReleaseNotes.orEmpty().map { (locale, notes) ->
                LocalizedText().apply {
                    language = locale
                    text = notes
                }
            }
        val existingReleaseNotes = this.releaseNotes.orEmpty()

        this.releaseNotes =
            if (existingReleaseNotes.isEmpty()) {
                releaseNotes
            } else {
                val merged = releaseNotes.toMutableList()

                for (existing in existingReleaseNotes) {
                    if (merged.none { it.language == existing.language }) merged += existing
                }

                merged
            }
    }

    private fun TrackRelease.updateUserFraction(userFraction: Double?) {
        if (userFraction != null) {
            this.userFraction = userFraction.takeIf { isRollout() }
        } else if (isRollout() && this.userFraction == null) {
            this.userFraction = DEFAULT_USER_FRACTION
        } else if (!isRollout()) {
            this.userFraction = null
        }
    }

    private fun TrackRelease.updateUpdatePriority(updatePriority: Int?) {
        if (updatePriority != null) {
            inAppUpdatePriority = updatePriority
        }
    }

    private fun ReleaseStatus.isRollout() = this == ReleaseStatus.IN_PROGRESS || this == ReleaseStatus.HALTED

    private fun TrackRelease.isRollout() =
        status == ReleaseStatus.IN_PROGRESS.publishedName ||
            status == ReleaseStatus.HALTED.publishedName

    private fun ReleaseStatus?.orDefault() = this ?: DEFAULT_RELEASE_STATUS

    private companion object {
        const val DEFAULT_USER_FRACTION = 0.1
        val DEFAULT_RELEASE_STATUS = ReleaseStatus.COMPLETED
    }
}
