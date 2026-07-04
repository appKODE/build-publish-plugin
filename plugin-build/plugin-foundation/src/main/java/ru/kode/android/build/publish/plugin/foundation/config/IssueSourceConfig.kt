package ru.kode.android.build.publish.plugin.foundation.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable

/**
 * Configuration for a single issue-tracker source used when processing the changelog.
 *
 * Declared inside `changelog { … { issueSources { … } } }`. Each source pairs a [numberPattern] that
 * recognizes issue keys with the [urlPrefix] those keys link to. Declaring several sources lets one
 * changelog reference issues from different projects and/or different issue-tracker hosts.
 */
interface IssueSourceConfig : CommonConfigMergeable<IssueSourceConfig> {
    val name: String

    /**
     * The regular expression that identifies issue keys of this source in commit messages
     * (Java regex syntax), e.g. `"BASE-\\d+"`. The full match is used as the issue key.
     */
    @get:Input
    val numberPattern: Property<String>

    /**
     * The base URL issue keys of this source are appended to when building links in the changelog
     * (e.g. `"https://jira.example.com/browse/"` → `https://jira.example.com/browse/BASE-123`).
     *
     * Optional: when unset, keys of this source are still extracted (for Jira/ClickUp automation)
     * but are not turned into links in Slack/Telegram messages.
     */
    @get:Input
    @get:Optional
    val urlPrefix: Property<String>

    override fun inheritFrom(common: IssueSourceConfig) {
        numberPattern.convention(common.numberPattern)
        urlPrefix.convention(common.urlPrefix)
    }
}
