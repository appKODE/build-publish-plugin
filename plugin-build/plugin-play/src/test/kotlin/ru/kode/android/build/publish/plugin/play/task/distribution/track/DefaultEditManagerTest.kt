package ru.kode.android.build.publish.plugin.play.task.distribution.track

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.androidpublisher.model.Bundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.FakeInternalPlayPublisher
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.ResolutionStrategy
import ru.kode.android.build.publish.plugin.play.task.distribution.publisher.googleJsonException
import java.io.File

class DefaultEditManagerTest {
    private val publisher = FakeInternalPlayPublisher()
    private val manager = DefaultEditManager(publisher, DefaultTrackManager(publisher, EDIT_ID), EDIT_ID)
    private val bundleFile = File("app-release.aab")

    @Test
    fun `should return uploaded version code on successful upload`() {
        publisher.uploadBundleResult = Bundle().apply { versionCode = 42 }

        val result = manager.uploadBundle(bundleFile, ResolutionStrategy.IGNORE)

        assertEquals(42L, result)
    }

    @Test
    fun `should ignore version conflict when strategy is ignore`() {
        publisher.uploadBundleError = googleJsonException("apkUpgradeVersionConflict")

        val result = manager.uploadBundle(bundleFile, ResolutionStrategy.IGNORE)

        assertNull(result)
    }

    @Test
    fun `should fail on version conflict when strategy is fail`() {
        publisher.uploadBundleError = googleJsonException("apkUpgradeVersionConflict")

        val error =
            assertThrows(IllegalStateException::class.java) {
                manager.uploadBundle(bundleFile, ResolutionStrategy.FAIL)
            }
        assertEquals(true, error.message!!.contains("too low or has already been used"))
    }

    @Test
    fun `should fail on version conflict when strategy is auto`() {
        publisher.uploadBundleError = googleJsonException("apkUpgradeVersionConflict")

        val error =
            assertThrows(IllegalStateException::class.java) {
                manager.uploadBundle(bundleFile, ResolutionStrategy.AUTO)
            }
        assertEquals(true, error.message!!.contains("Concurrent uploads"))
    }

    @Test
    fun `should fail on version conflict when strategy is auto offset`() {
        publisher.uploadBundleError = googleJsonException("apkNotificationMessageKeyUpgradeVersionConflict")

        assertThrows(IllegalStateException::class.java) {
            manager.uploadBundle(bundleFile, ResolutionStrategy.AUTO_OFFSET)
        }
    }

    @Test
    fun `should ignore forbidden failure when message reports version code conflict`() {
        publisher.uploadBundleError =
            googleJsonException(
                reason = "forbidden",
                message = "APK specifies a version code that has already been used.",
            )

        val result = manager.uploadBundle(bundleFile, ResolutionStrategy.IGNORE)

        assertNull(result)
    }

    @Test
    fun `should rethrow forbidden failure when message is unrelated`() {
        publisher.uploadBundleError =
            googleJsonException(reason = "forbidden", message = "You do not have access.")

        assertThrows(GoogleJsonResponseException::class.java) {
            manager.uploadBundle(bundleFile, ResolutionStrategy.IGNORE)
        }
    }

    @Test
    fun `should rethrow unrelated failure reasons`() {
        publisher.uploadBundleError = googleJsonException("internalError")

        assertThrows(GoogleJsonResponseException::class.java) {
            manager.uploadBundle(bundleFile, ResolutionStrategy.IGNORE)
        }
    }

    private companion object {
        const val EDIT_ID = "edit-1"
    }
}
