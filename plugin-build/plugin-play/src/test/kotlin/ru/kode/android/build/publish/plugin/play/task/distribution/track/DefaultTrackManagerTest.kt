package ru.kode.android.build.publish.plugin.play.task.distribution.track

import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.FakeInternalPlayPublisher
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.ReleaseStatus

private const val EDIT_ID = "edit-1"

class DefaultTrackManagerTest {
    private val publisher = FakeInternalPlayPublisher()
    private val manager = DefaultTrackManager(publisher, EDIT_ID)

    // region promote

    @Test
    fun `should fail promotion when source track has no releases`() {
        val error =
            assertThrows(IllegalStateException::class.java) {
                manager.promote(promoteConfig(from = "internal", to = "production"))
            }
        assertTrue(error.message!!.contains("has no releases"))
    }

    @Test
    fun `should retarget track to promote target on promotion`() {
        publisher.tracks["internal"] =
            track("internal", release(ReleaseStatus.COMPLETED, versionCodes = listOf(1L)))

        manager.promote(promoteConfig(from = "internal", to = "production"))

        assertEquals("production", publisher.updatedTracks.single().track)
    }

    @Test
    fun `should keep highest version code when deduplicating identical statuses on promotion`() {
        publisher.tracks["internal"] =
            track(
                "internal",
                release(ReleaseStatus.COMPLETED, versionCodes = listOf(1L)),
                release(ReleaseStatus.COMPLETED, versionCodes = listOf(2L)),
            )

        manager.promote(promoteConfig(from = "internal", to = "production"))

        val releases = publisher.updatedTracks.single().releases
        assertEquals(1, releases.size)
        assertEquals(listOf(2L), releases.single().versionCodes)
    }

    @Test
    fun `should keep distinct statuses on promotion`() {
        publisher.tracks["internal"] =
            track(
                "internal",
                release(ReleaseStatus.IN_PROGRESS, versionCodes = listOf(1L)),
                release(ReleaseStatus.COMPLETED, versionCodes = listOf(2L)),
            )

        manager.promote(promoteConfig(from = "internal", to = "production"))

        val statuses = publisher.updatedTracks.single().releases.map { it.status }
        assertEquals(setOf("completed", "inProgress"), statuses.toSet())
    }

    // endregion

    // region rollout

    @Test
    fun `should apply default user fraction for in-progress rollout`() {
        manager.update(
            updateConfig(
                trackName = "production",
                versionCodes = listOf(5L),
                releaseStatus = ReleaseStatus.IN_PROGRESS,
            ),
        )

        val release = publisher.updatedTracks.single().releases.single()
        assertEquals("inProgress", release.status)
        assertEquals(0.1, release.userFraction)
    }

    @Test
    fun `should treat halted status as rollout`() {
        manager.update(
            updateConfig(
                trackName = "production",
                versionCodes = listOf(5L),
                releaseStatus = ReleaseStatus.HALTED,
            ),
        )

        val release = publisher.updatedTracks.single().releases.single()
        assertEquals("halted", release.status)
        assertEquals(0.1, release.userFraction)
    }

    @Test
    fun `should clear user fraction when release is not a rollout`() {
        manager.update(
            updateConfig(
                trackName = "production",
                versionCodes = listOf(5L),
                releaseStatus = ReleaseStatus.COMPLETED,
                userFraction = 0.5,
            ),
        )

        val release = publisher.updatedTracks.single().releases.single()
        assertEquals("completed", release.status)
        assertNull(release.userFraction)
    }

    @Test
    fun `should preserve non-rollout releases when creating a rollout`() {
        publisher.tracks["production"] =
            track("production", release(ReleaseStatus.COMPLETED, versionCodes = listOf(3L)))

        manager.update(
            updateConfig(
                trackName = "production",
                versionCodes = listOf(5L),
                releaseStatus = ReleaseStatus.IN_PROGRESS,
            ),
        )

        val releases = publisher.updatedTracks.single().releases
        assertEquals(2, releases.size)
        assertTrue(releases.any { it.status == "completed" && it.versionCodes == listOf(3L) })
        assertTrue(releases.any { it.status == "inProgress" && it.versionCodes == listOf(5L) })
    }

    // endregion

    // region skipped commit

    @Test
    fun `should create a single release for skipped commit on an empty track`() {
        manager.update(
            updateConfig(
                trackName = "production",
                versionCodes = listOf(5L),
                didPreviousBuildSkipCommit = true,
            ),
        )

        val releases = publisher.updatedTracks.single().releases
        assertEquals(1, releases.size)
        assertEquals(listOf(5L), releases.single().versionCodes)
    }

    @Test
    fun `should merge version codes into matching status release for skipped commit`() {
        publisher.tracks["production"] =
            track("production", release(ReleaseStatus.COMPLETED, versionCodes = listOf(1L)))

        manager.update(
            updateConfig(
                trackName = "production",
                versionCodes = listOf(2L),
                releaseStatus = ReleaseStatus.COMPLETED,
                didPreviousBuildSkipCommit = true,
            ),
        )

        val releases = publisher.updatedTracks.single().releases
        assertEquals(1, releases.size)
        assertEquals(listOf(1L, 2L), releases.single().versionCodes)
    }

    @Test
    fun `should append new release for skipped commit when status does not match`() {
        publisher.tracks["production"] =
            track(
                "production",
                release(
                    ReleaseStatus.DRAFT,
                    versionCodes = listOf(1L),
                    notes = listOf(localizedText("en-US", "Draft notes")),
                ),
            )

        manager.update(
            updateConfig(
                trackName = "production",
                versionCodes = listOf(2L),
                releaseStatus = ReleaseStatus.COMPLETED,
                didPreviousBuildSkipCommit = true,
            ),
        )

        val releases = publisher.updatedTracks.single().releases
        assertEquals(2, releases.size)
        val appended = releases.single { it.status == "completed" }
        assertEquals(listOf(2L), appended.versionCodes)
    }

    @Test
    fun `should copy changelog from previous release when appending skipped-commit release`() {
        publisher.tracks["production"] =
            track(
                "production",
                release(
                    ReleaseStatus.DRAFT,
                    versionCodes = listOf(1L),
                    notes = listOf(localizedText("en-US", "Draft notes")),
                ),
            )

        manager.update(
            updateConfig(
                trackName = "production",
                versionCodes = listOf(2L),
                releaseStatus = ReleaseStatus.COMPLETED,
                didPreviousBuildSkipCommit = true,
            ),
        )

        val appended = publisher.updatedTracks.single().releases.single { it.status == "completed" }
        assertEquals(1, appended.releaseNotes.size)
        assertEquals("en-US", appended.releaseNotes.single().language)
        assertEquals("Draft notes", appended.releaseNotes.single().text)
    }

    // endregion

    // region release notes merge

    @Test
    fun `should preserve existing languages when merging release notes`() {
        publisher.tracks["production"] =
            track(
                "production",
                release(
                    ReleaseStatus.COMPLETED,
                    versionCodes = listOf(1L),
                    notes = listOf(localizedText("ru-RU", "Существующие")),
                ),
            )

        manager.update(
            updateConfig(
                trackName = "production",
                versionCodes = listOf(2L),
                releaseStatus = ReleaseStatus.COMPLETED,
                didPreviousBuildSkipCommit = true,
                releaseNotes = mapOf("en-US" to "New notes"),
            ),
        )

        val notes = publisher.updatedTracks.single().releases.single().releaseNotes
        assertEquals(
            mapOf("en-US" to "New notes", "ru-RU" to "Существующие"),
            notes.associate { it.language to it.text },
        )
    }

    // endregion

    private fun promoteConfig(
        from: String,
        to: String,
    ) = TrackManager.PromoteConfig(
        promoteTrackName = to,
        fromTrackName = from,
        versionCode = null,
        base = baseConfig(),
    )

    private fun updateConfig(
        trackName: String,
        versionCodes: List<Long>,
        didPreviousBuildSkipCommit: Boolean = false,
        releaseStatus: ReleaseStatus? = null,
        userFraction: Double? = null,
        releaseNotes: Map<String, String?>? = emptyMap(),
    ) = TrackManager.UpdateConfig(
        trackName = trackName,
        versionCodes = versionCodes,
        didPreviousBuildSkipCommit = didPreviousBuildSkipCommit,
        base =
            baseConfig(
                releaseStatus = releaseStatus,
                userFraction = userFraction,
                releaseNotes = releaseNotes,
            ),
    )

    private fun baseConfig(
        releaseStatus: ReleaseStatus? = null,
        userFraction: Double? = null,
        releaseNotes: Map<String, String?>? = emptyMap(),
    ) = TrackManager.BaseConfig(
        releaseStatus = releaseStatus,
        userFraction = userFraction,
        updatePriority = null,
        releaseNotes = releaseNotes,
        retainableArtifacts = null,
        releaseName = null,
    )

    private fun track(
        name: String,
        vararg releases: TrackRelease,
    ) = Track().apply {
        this.track = name
        this.releases = releases.toList()
    }

    private fun release(
        status: ReleaseStatus,
        versionCodes: List<Long>,
        notes: List<LocalizedText> = emptyList(),
    ) = TrackRelease().apply {
        this.status = status.publishedName
        this.versionCodes = versionCodes
        if (notes.isNotEmpty()) this.releaseNotes = notes
    }

    private fun localizedText(
        language: String,
        text: String,
    ) = LocalizedText().apply {
        this.language = language
        this.text = text
    }
}
