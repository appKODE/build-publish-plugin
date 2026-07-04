package ru.kode.android.build.publish.plugin.core.enity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * [IssueReference] is carried as a `@Input ListProperty<IssueReference>` on the changelog task, so Gradle
 * fingerprints it via Java serialization for up-to-date checks and the build cache. A non-serializable
 * value fails the task at execution time (`NotSerializableException`), so this guards that contract at the
 * fast unit-test layer instead of only in the live integration suite.
 */
class IssueReferenceSerializationTest {
    @Test
    fun `IssueReference is Java-serializable and round-trips`() {
        val original = IssueReference(key = "CLOSES", numberPattern = "(\\d+|[A-Z]+-\\d+)")

        val restored = roundTrip(original)

        assertEquals(original, restored)
    }

    private fun <T : Serializable> roundTrip(value: T): Any? {
        val bytes =
            ByteArrayOutputStream().use { byteStream ->
                ObjectOutputStream(byteStream).use { it.writeObject(value) }
                byteStream.toByteArray()
            }
        return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
    }
}
