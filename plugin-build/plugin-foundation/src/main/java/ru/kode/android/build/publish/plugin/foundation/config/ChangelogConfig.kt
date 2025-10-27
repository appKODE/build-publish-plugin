package ru.kode.android.build.publish.plugin.foundation.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Configuration interface for changelog generation settings.
 *
 * This interface defines the configuration options for generating changelogs from
 * Git commit history. It allows customization of how issue tracking is integrated
 * and which commits should be included in the changelog.
 */
interface ChangelogConfig {
    val name: String

    /**
     * The regular expression pattern used to identify issue/ticket numbers in commit messages.
     *
     * This pattern is used to extract issue references from commit messages and link them
     * to your issue tracker. The pattern should follow Java regex syntax.
     *
     * Example: `"TICKET-\\d+"` would match commit messages like:
     * - "TICKET-123: Fix login issue"
     * - "feat: Add new feature (TICKET-456)"
     *
     * @see java.util.regex.Pattern
     */
    @get:Input
    val issueNumberPattern: Property<String>

    /**
     * The base URL of your issue tracker.
     *
     * This URL is used to generate clickable links to issues in the generated changelog.
     * The issue number extracted using [issueNumberPattern] will be appended to this URL.
     *
     * Example: `"https://jira.example.com/browse/"` would generate links like
     * `https://jira.example.com/browse/TICKET-123`
     */
    @get:Input
    val issueUrlPrefix: Property<String>

    /**
     * The commit message key used to identify commits that should be included in the changelog.
     *
     * Only commits that contain this key in their message will be included in the
     * generated changelog. This allows you to selectively include only relevant commits.
     *
     * Example: If set to `"CHANGELOG"`, only commits containing `[CHANGELOG]` in their
     * message will be included in the changelog.
     *
     * Note: The comparison is case-sensitive.
     */
    @get:Input
    val commitMessageKey: Property<String>

    /**
     * If `true`, remove the configured [commitMessageKey] from commit messages
     * when generating the changelog.
     *
     * The [commitMessageKey] is looked up in each commit message to decide which commits are relevant
     * for the changelog. When this property is enabled (`true`) or unset,
     * occurrences of the key will be stripped from the commit message text
     * in the generated changelog output. When disabled (`false`),
     * the key is left intact in the commit messages.
     *
     * This is useful to avoid showing internal markers (for example `[CHANGELOG]` or `[SKIP]`) in the
     * final changelog while still using them to identify relevant commits.
     *
     * Example:
     * - `commitMessageKey = "CHANGELOG"`
     * - `excludeMessageKey = true`
     *
     * A commit message like `"[CHANGELOG] Fix crash on start"` will appear as `"Fix crash on start"`
     * in the generated changelog.
     *
     * Default: `true`
     */
    @get:Input
    @get:Optional
    val excludeMessageKey: Property<Boolean>
}
