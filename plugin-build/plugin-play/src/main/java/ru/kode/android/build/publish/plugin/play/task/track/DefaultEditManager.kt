package ru.kode.android.build.publish.plugin.play.task.track

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import org.slf4j.LoggerFactory
import ru.kode.android.build.publish.plugin.play.task.publisher.InternalPlayPublisher
import ru.kode.android.build.publish.plugin.play.task.publisher.ReleaseStatus
import ru.kode.android.build.publish.plugin.play.task.publisher.ResolutionStrategy
import ru.kode.android.build.publish.plugin.play.task.publisher.has
import java.io.File

internal class DefaultEditManager(
    private val publisher: InternalPlayPublisher,
    private val tracks: TrackManager,
    private val editId: String,
) : EditManager {
    override fun promoteRelease(
        promoteTrackName: String,
        fromTrackName: String,
        releaseStatus: ReleaseStatus?,
        releaseName: String?,
        releaseNotes: Map<String, String?>?,
        userFraction: Double?,
        updatePriority: Int?,
        retainableArtifacts: List<Long>?,
        versionCode: Long?,
    ) {
        tracks.promote(
            TrackManager.PromoteConfig(
                promoteTrackName,
                fromTrackName,
                versionCode,
                TrackManager.BaseConfig(
                    releaseStatus,
                    userFraction,
                    updatePriority,
                    releaseNotes,
                    retainableArtifacts,
                    releaseName,
                ),
            ),
        )
    }

    override fun uploadBundle(
        bundleFile: File,
        strategy: ResolutionStrategy,
    ): Long? {
        val bundle =
            try {
                publisher.uploadBundle(editId, bundleFile)
            } catch (e: GoogleJsonResponseException) {
                handleUploadFailures(e, strategy, bundleFile)
                return null
            }

        return bundle.versionCode.toLong()
    }

    override fun publishArtifacts(
        versionCodes: List<Long>,
        didPreviousBuildSkipCommit: Boolean,
        trackName: String,
        releaseStatus: ReleaseStatus?,
        releaseName: String?,
        releaseNotes: Map<String, String?>?,
        userFraction: Double?,
        updatePriority: Int?,
        retainableArtifacts: List<Long>?,
    ) {
        if (versionCodes.isEmpty()) return

        tracks.update(
            TrackManager.UpdateConfig(
                trackName,
                versionCodes,
                didPreviousBuildSkipCommit,
                TrackManager.BaseConfig(
                    releaseStatus,
                    userFraction,
                    updatePriority,
                    releaseNotes,
                    retainableArtifacts,
                    releaseName,
                ),
            ),
        )
    }

    @Suppress("ComplexCondition") // API response code handling
    private fun handleUploadFailures(
        e: GoogleJsonResponseException,
        strategy: ResolutionStrategy,
        artifact: File,
    ): Nothing? =
        if (
            e has "apkNotificationMessageKeyUpgradeVersionConflict" ||
            e has "apkUpgradeVersionConflict" ||
            e has "apkNoUpgradePath" ||
            e has "forbidden" &&
            e.details.message.orEmpty().let { m ->
                // Bundle message: APK specifies a version code that has already been used.
                // APK message: Cannot update a published APK.
                m.contains("version code", ignoreCase = true) || m.contains("Cannot update", ignoreCase = true)
            }
        ) {
            when (strategy) {
                ResolutionStrategy.AUTO, ResolutionStrategy.AUTO_OFFSET -> throw IllegalStateException(
                    "Concurrent uploads for app ${publisher.appId} (version code " +
                        "already used). Make sure to synchronously upload your APKs such " +
                        "that they don't conflict. If this problem persists, delete your " +
                        "drafts in the Play Console's artifact library.",
                    e,
                )

                ResolutionStrategy.FAIL -> throw IllegalStateException(
                    "Version code is too low or has already been used for app " +
                        "${publisher.appId}.",
                    e,
                )

                ResolutionStrategy.IGNORE ->
                    LoggerFactory.getLogger(EditManager::class.java).warn(
                        "Ignoring artifact ($artifact)",
                    )
            }
            null
        } else {
            throw e
        }
}
