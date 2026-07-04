package ru.kode.android.build.publish.plugin.foundation.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable

/**
 * Configuration for a single issue-reference marker used to derive changelog entries from commit
 * messages.
 *
 * Declared inside `changelog { … { issueReferences { … } } }`. Each reference names a commit marker
 * ([key], e.g. `CLOSES`/`FIXES`) and a [numberPattern] that extracts the issue token following it, so a
 * provider plugin (Jira, ClickUp, …) can fetch the title automatically instead of it being copy-pasted
 * into a `CHANGELOG:` line.
 */
interface IssueReferenceConfig : CommonConfigMergeable<IssueReferenceConfig> {
    val name: String

    /**
     * The commit marker keyword whose lines are treated as issue references, e.g. `"CLOSES"` or
     * `"FIXES"`. Only lines containing this exact string are considered.
     */
    @get:Input
    val key: Property<String>

    /**
     * The regular expression that extracts the issue token after the marker (Java regex syntax),
     * e.g. `"(\\d+|[A-Z]+-\\d+)"`. The first match on the line is used as the reference token.
     */
    @get:Input
    val numberPattern: Property<String>

    override fun inheritFrom(common: IssueReferenceConfig) {
        key.convention(common.key)
        numberPattern.convention(common.numberPattern)
    }
}
