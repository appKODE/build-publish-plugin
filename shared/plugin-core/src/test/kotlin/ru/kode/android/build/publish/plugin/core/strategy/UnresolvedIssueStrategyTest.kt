package ru.kode.android.build.publish.plugin.core.strategy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UnresolvedIssueStrategyTest {
    @Test
    fun `ChangelogLineOrKeyUnresolvedStrategy yields key when commit has no changelog line`() {
        val line = ChangelogLineOrKeyUnresolvedStrategy.build("TBI-3458", commitChangelogLine = null)

        assertEquals("• [TBI-3458]", line)
    }

    @Test
    fun `ChangelogLineOrKeyUnresolvedStrategy defers to the existing changelog line`() {
        val line = ChangelogLineOrKeyUnresolvedStrategy.build("TBI-3458", commitChangelogLine = "CHANGELOG: manual")

        assertNull(line)
    }

    @Test
    fun `KeyOnlyUnresolvedStrategy always renders the key`() {
        assertEquals("• [TBI-3458]", KeyOnlyUnresolvedStrategy.build("TBI-3458", commitChangelogLine = "CHANGELOG: x"))
    }

    @Test
    fun `SkipUnresolvedStrategy omits the entry`() {
        assertNull(SkipUnresolvedStrategy.build("TBI-3458", commitChangelogLine = null))
    }

    @Test
    fun `FallbackTextUnresolvedStrategy renders key with the fixed text`() {
        val line = FallbackTextUnresolvedStrategy("(no description)").build("TBI-3458", commitChangelogLine = null)

        assertEquals("• [TBI-3458] (no description)", line)
    }
}
