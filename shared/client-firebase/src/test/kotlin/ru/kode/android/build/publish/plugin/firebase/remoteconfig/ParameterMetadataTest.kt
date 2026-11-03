package ru.kode.android.build.publish.plugin.firebase.remoteconfig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ParameterMetadataTest {
    @Test
    fun `should parse metadata from description`() {
        assertEquals(
            ParameterMetadata("@ivan", "release", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 1)),
            parseMetadata("Checkout redesign. owner=@ivan type=release created=2026-07-01 expires=2026-12-01"),
        )
    }

    @Test
    fun `should treat invalid dates and missing description as absent`() {
        assertEquals(ParameterMetadata(owner = "@ivan"), parseMetadata("owner=@ivan created=soon expires=2026-13-01"))
        assertEquals(ParameterMetadata(), parseMetadata(null))
    }
}
