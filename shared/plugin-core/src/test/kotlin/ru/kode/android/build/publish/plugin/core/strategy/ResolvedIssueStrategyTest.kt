package ru.kode.android.build.publish.plugin.core.strategy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResolvedIssueStrategyTest {
    @Test
    fun `KeyAndTitleResolvedStrategy renders key and title`() {
        val line = KeyAndTitleResolvedStrategy.build("TBI-3458", "Fix cold start", commitChangelogLine = null)

        assertEquals("• [TBI-3458] Fix cold start", line)
    }

    @Test
    fun `TitleOnlyResolvedStrategy drops the key`() {
        val line = TitleOnlyResolvedStrategy.build("TBI-3458", "Fix cold start", commitChangelogLine = null)

        assertEquals("• Fix cold start", line)
    }

    @Test
    fun `KeyOnlyResolvedStrategy drops the title`() {
        val line = KeyOnlyResolvedStrategy.build("TBI-3458", "Fix cold start", commitChangelogLine = null)

        assertEquals("• [TBI-3458]", line)
    }
}
