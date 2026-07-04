package ru.kode.android.build.publish.plugin.core.enity

/**
 * A single issue-reference marker used to derive changelog entries from commit messages.
 *
 * Where [IssueSource] recognizes issue keys already written in a changelog line (for linking), an
 * [IssueReference] recognizes a commit marker such as `CLOSES` or `FIXES` and extracts the issue token
 * that follows it, so the entry (and its title) can be resolved automatically.
 *
 * @property key The commit marker keyword (e.g. `"CLOSES"`, `"FIXES"`). Only lines containing this exact
 *   string are treated as references.
 * @property numberPattern Java regex matching the issue token after the marker, e.g. `"(\\d+|[A-Z]+-\\d+)"`.
 *   The first match on the line is used as the reference token.
 */
data class IssueReference(
    val key: String,
    val numberPattern: String,
)
