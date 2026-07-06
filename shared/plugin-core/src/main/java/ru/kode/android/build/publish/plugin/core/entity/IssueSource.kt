package ru.kode.android.build.publish.plugin.core.entity

import kotlinx.serialization.Serializable

/**
 * A single issue-tracker source used when processing the changelog.
 *
 * Each source pairs a regex ([numberPattern]) that recognizes issue keys in commit messages with the
 * base URL ([urlPrefix]) those keys link to. Multiple sources let a single changelog reference issues
 * that live in different projects and/or on different issue-tracker hosts: extraction unions every
 * source's pattern, and link formatting resolves each matched key against its own source's URL.
 *
 * @property numberPattern Java regex matching issue keys (e.g. `"BASE-\\d+"`).
 * @property urlPrefix Base URL the matched key is appended to (e.g. `"https://jira/browse/"`);
 *   `null`/blank means the source is used for extraction only and its keys are not linked.
 */
@Serializable
data class IssueSource(
    val numberPattern: String,
    val urlPrefix: String?,
)
