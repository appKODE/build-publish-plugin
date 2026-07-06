package ru.kode.android.build.publish.plugin.core.git

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Person
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.entity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.core.entity.IssueReference
import ru.kode.android.build.publish.plugin.core.entity.Tag
import ru.kode.android.build.publish.plugin.core.issue.IssueResolver
import ru.kode.android.build.publish.plugin.core.issue.ResolvedIssue
import ru.kode.android.build.publish.plugin.core.logger.pluginLoggerFromLog
import ru.kode.android.build.publish.plugin.core.strategy.ChangelogLineOrKeyUnresolvedStrategy
import ru.kode.android.build.publish.plugin.core.strategy.KeyAndTitleResolvedStrategy
import ru.kode.android.build.publish.plugin.core.strategy.KeyOnlyResolvedStrategy
import ru.kode.android.build.publish.plugin.core.strategy.KeyOnlyUnresolvedStrategy
import ru.kode.android.build.publish.plugin.core.strategy.ResolvedIssueStrategy
import ru.kode.android.build.publish.plugin.core.strategy.SkipUnresolvedStrategy
import ru.kode.android.build.publish.plugin.core.strategy.TitleOnlyResolvedStrategy
import ru.kode.android.build.publish.plugin.core.strategy.UnresolvedIssueStrategy
import java.io.File

class GitChangelogBuilderTest {
    @TempDir
    lateinit var tempDir: File

    private var grgit: Grgit? = null

    private val references =
        listOf(
            IssueReference(key = "CLOSES", numberPattern = "(\\d+|[A-Z]+-\\d+)"),
            IssueReference(key = "FIXES", numberPattern = "(\\d+|[A-Z]+-\\d+)"),
        )

    private val logger = pluginLoggerFromLog { }

    @AfterEach
    fun tearDown() {
        grgit?.close()
    }

    /**
     * A fake resolver that resolves a fixed set of tokens/keys and can optionally throw for a token to
     * verify the builder swallows failures.
     */
    private class FakeResolver(
        private val titles: Map<String, ResolvedIssue> = emptyMap(),
        private val throwOn: Set<String> = emptySet(),
    ) : IssueResolver {
        override fun resolve(reference: String): ResolvedIssue? {
            if (reference in throwOn) error("boom for $reference")
            return titles[reference]
        }
    }

    private fun build(
        commitMessages: List<String>,
        resolvers: List<IssueResolver>,
        resolvedStrategy: ResolvedIssueStrategy = KeyAndTitleResolvedStrategy,
        unresolvedStrategy: UnresolvedIssueStrategy = ChangelogLineOrKeyUnresolvedStrategy,
    ): String? {
        val git = Grgit.init(mapOf("dir" to tempDir))
        grgit = git
        val person = Person("tester", "tester@example.com")

        File(tempDir, "seed").writeText("seed")
        git.add(mapOf("patterns" to setOf(".")))
        git.commit(mapOf("message" to "init", "author" to person, "committer" to person))
        val previousSha = git.head().id

        commitMessages.forEachIndexed { index, message ->
            File(tempDir, "file$index").writeText("$index")
            git.add(mapOf("patterns" to setOf(".")))
            git.commit(mapOf("message" to message, "author" to person, "committer" to person))
        }
        val currentSha = git.head().id

        val snapshot =
            BuildTagSnapshot(
                current = buildTag(currentSha, 2),
                previousInOrder = buildTag(previousSha, 1),
                previousOnDifferentCommit = buildTag(previousSha, 1),
            )

        val repository = GitRepository(GitCommandExecutor(git, logger))
        return GitChangelogBuilder(repository, logger)
            .buildForSnapshot(
                messageKey = "CHANGELOG",
                annotatedTagMessageBuilder = { it },
                messageBuilder = { line -> "• ${line.substringAfter("CHANGELOG:").trim()}" },
                tagSnapshot = snapshot,
                issueReferences = references,
                resolvers = resolvers,
                resolvedStrategy = resolvedStrategy,
                unresolvedStrategy = unresolvedStrategy,
            )
    }

    private fun buildTag(
        commitSha: String,
        number: Int,
    ) = Tag.Build(
        name = "app.$number-debug",
        commitSha = commitSha,
        message = null,
        buildVersion = number.toString(),
        buildVariant = "debug",
        buildNumber = number,
    )

    @Test
    fun `resolves bare and prefixed references to titles`() {
        val changelog =
            build(
                commitMessages =
                    listOf(
                        "Add cold start fix\n\nCLOSES: 3458",
                        "Fix publishing\n\nFIXES: TBI-3459",
                    ),
                resolvers =
                    listOf(
                        FakeResolver(
                            titles =
                                mapOf(
                                    "3458" to ResolvedIssue("TBI-3458", "Fix cold start"),
                                    "TBI-3459" to ResolvedIssue("TBI-3459", "Fix publishing flow"),
                                ),
                        ),
                    ),
            )

        assertTrue(changelog!!.contains("• [TBI-3458] Fix cold start"), changelog)
        assertTrue(changelog.contains("• [TBI-3459] Fix publishing flow"), changelog)
    }

    @Test
    fun `first non-null resolver wins`() {
        val changelog =
            build(
                commitMessages = listOf("Feature\n\nCLOSES: 3458"),
                resolvers =
                    listOf(
                        FakeResolver(titles = emptyMap()),
                        FakeResolver(titles = mapOf("3458" to ResolvedIssue("TBI-3458", "Second resolver"))),
                    ),
            )

        assertTrue(changelog!!.contains("• [TBI-3458] Second resolver"), changelog)
    }

    @Test
    fun `throwing resolver is swallowed and the next resolver is used`() {
        val changelog =
            build(
                commitMessages = listOf("Feature\n\nCLOSES: 3458"),
                resolvers =
                    listOf(
                        FakeResolver(throwOn = setOf("3458")),
                        FakeResolver(titles = mapOf("3458" to ResolvedIssue("TBI-3458", "Recovered"))),
                    ),
            )

        assertTrue(changelog!!.contains("• [TBI-3458] Recovered"), changelog)
    }

    @Test
    fun `manual changelog line dedupes the matching reference`() {
        val changelog =
            build(
                commitMessages = listOf("Manual\n\nCHANGELOG: [999] Manual entry\nCLOSES: 999"),
                resolvers = listOf(FakeResolver(titles = mapOf("999" to ResolvedIssue("APP-999", "Fetched title")))),
            )

        assertTrue(changelog!!.contains("• [999] Manual entry"), changelog)
        assertFalse(changelog.contains("Fetched title"), changelog)
    }

    @Test
    fun `resolvedStrategy controls the resolved rendering`() {
        val titleOnly =
            build(
                commitMessages = listOf("Feature\n\nCLOSES: 3458"),
                resolvers = listOf(FakeResolver(titles = mapOf("3458" to ResolvedIssue("TBI-3458", "Fix cold start")))),
                resolvedStrategy = TitleOnlyResolvedStrategy,
            )
        assertTrue(titleOnly!!.contains("• Fix cold start"), titleOnly)
        assertFalse(titleOnly.contains("TBI-3458"), titleOnly)

        val keyOnly =
            build(
                commitMessages = listOf("Feature\n\nCLOSES: 3458"),
                resolvers = listOf(FakeResolver(titles = mapOf("3458" to ResolvedIssue("TBI-3458", "Fix cold start")))),
                resolvedStrategy = KeyOnlyResolvedStrategy,
            )
        assertTrue(keyOnly!!.contains("• [TBI-3458]"), keyOnly)
        assertFalse(keyOnly.contains("Fix cold start"), keyOnly)
    }

    @Test
    fun `unresolved reference falls back to the unresolved strategy`() {
        val keyOnly =
            build(
                commitMessages = listOf("Feature\n\nCLOSES: 7777"),
                resolvers = listOf(FakeResolver(titles = emptyMap())),
                unresolvedStrategy = KeyOnlyUnresolvedStrategy,
            )
        assertTrue(keyOnly!!.contains("• [7777]"), keyOnly)
    }

    @Test
    fun `skip unresolved strategy omits the entry`() {
        val changelog =
            build(
                commitMessages = listOf("Feature\n\nCLOSES: 7777"),
                resolvers = listOf(FakeResolver(titles = emptyMap())),
                unresolvedStrategy = SkipUnresolvedStrategy,
            )
        assertFalse(changelog.orEmpty().contains("7777"), changelog.orEmpty())
    }

    @Test
    fun `marker embedded in a larger word is not treated as a reference`() {
        val changelog =
            build(
                // "DISCLOSES" contains "CLOSES" as a substring but is not the CLOSES marker
                commitMessages = listOf("Docs\n\nDISCLOSES: 3458"),
                resolvers = listOf(FakeResolver(titles = mapOf("3458" to ResolvedIssue("TBI-3458", "Should not appear")))),
            )

        assertNull(changelog)
    }

    @Test
    fun `marker is matched at a word boundary and resolves`() {
        val changelog =
            build(
                commitMessages = listOf("Fix\n\nCLOSES: 3458"),
                resolvers = listOf(FakeResolver(titles = mapOf("3458" to ResolvedIssue("TBI-3458", "Fix cold start")))),
            )

        assertTrue(changelog!!.contains("• [TBI-3458] Fix cold start"), changelog)
    }

    @Test
    fun `no resolvers leaves the reference pass disabled`() {
        val changelog =
            build(
                commitMessages = listOf("Feature\n\nCLOSES: 3458"),
                resolvers = emptyList(),
            )
        assertNull(changelog)
    }
}
