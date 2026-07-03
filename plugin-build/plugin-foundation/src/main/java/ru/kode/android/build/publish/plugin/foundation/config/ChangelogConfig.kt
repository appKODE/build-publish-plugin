package ru.kode.android.build.publish.plugin.foundation.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import ru.kode.android.build.publish.plugin.core.strategy.AnnotatedTagMessageStrategy
import ru.kode.android.build.publish.plugin.core.strategy.ChangelogMessageStrategy
import ru.kode.android.build.publish.plugin.core.strategy.EmptyChangelogMessageStrategy
import ru.kode.android.build.publish.plugin.core.strategy.NotGeneratedChangelogMessageStrategy
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy

/**
 * Configuration interface for changelog generation settings.
 *
 * This interface defines the configuration options for generating changelogs from
 * Git commit history. It allows customization of how issue tracking is integrated
 * and which commits should be included in the changelog.
 */
abstract class ChangelogConfig : CommonConfigMergeable<ChangelogConfig> {
    abstract val name: String

    /**
     * Issue-tracker sources used when processing the changelog.
     *
     * Configure sources through the [issueSources] method rather than accessing this directly.
     *
     * @see issueSources
     * @see IssueSourceConfig
     */
    @get:Nested
    abstract val issueSourcesConfig: IssueSourcesConfig

    /**
     * Configures the issue-tracker sources used when processing the changelog.
     *
     * Each source (added via [IssueSourcesConfig.issueSource]) pairs a regex that recognizes issue
     * keys with the URL those keys link to. Declaring several sources lets one changelog reference
     * issues from different projects and/or different issue-tracker hosts — extraction (Jira/ClickUp)
     * unions every source's `numberPattern`, and link formatting (Slack/Telegram) resolves each
     * matched key against its own source's `urlPrefix`.
     *
     * Example:
     * ```
     * issueSources {
     *     issueSource("base") { numberPattern.set("BASE-\\d+"); urlPrefix.set("https://jira1/browse/") }
     *     issueSource("leg")  { numberPattern.set("LEG-\\d+");  urlPrefix.set("https://jira2/browse/") }
     * }
     * ```
     *
     * @param action A configuration block applied to the [IssueSourcesConfig] container.
     *
     * @see IssueSourcesConfig
     */
    fun issueSources(action: Action<IssueSourcesConfig>) {
        action.execute(issueSourcesConfig)
    }

    /**
     * Configures the issue-tracker sources using a Groovy closure.
     *
     * @param configurationClosure The Groovy closure applied to the [IssueSourcesConfig] container.
     *
     * @see issueSources
     */
    fun issueSources(
        @DelegatesTo(value = IssueSourcesConfig::class, strategy = Closure.DELEGATE_FIRST)
        configurationClosure: Closure<in IssueSourcesConfig>,
    ) {
        issueSources { target -> configureGroovy(configurationClosure, target) }
    }

    /**
     * Shorthand for declaring a single issue source without the surrounding `issueSources { }` block.
     *
     * Equivalent to `issueSources { issueSource(name) { … } }`. Use the [issueSources] block instead
     * when the changelog references issues from more than one source.
     *
     * @param name A unique identifier for the source (e.g. "base").
     * @param action A configuration block applied to the new [IssueSourceConfig] instance.
     */
    fun issueSource(
        name: String,
        action: Action<IssueSourceConfig>,
    ) {
        issueSourcesConfig.issueSource(name, action)
    }

    /**
     * Shorthand for declaring a single issue source with the given name using a Groovy closure.
     *
     * @param name A unique identifier for the source (e.g. "base").
     * @param configurationClosure The Groovy closure applied to the new [IssueSourceConfig].
     *
     * @see issueSource
     */
    fun issueSource(
        name: String,
        @DelegatesTo(value = IssueSourceConfig::class, strategy = Closure.DELEGATE_FIRST)
        configurationClosure: Closure<in IssueSourceConfig>,
    ) {
        issueSource(name) { target -> configureGroovy(configurationClosure, target) }
    }

    /**
     * Shorthand for declaring the single issue source of this changelog without the surrounding
     * `issueSources { }` block and without naming it.
     *
     * Equivalent to `issueSources { issueSource("default") { … } }`. Use the named [issueSource] or
     * the [issueSources] block when the changelog references issues from more than one source.
     *
     * @param action A configuration block applied to the new [IssueSourceConfig] instance.
     */
    fun issueSource(action: Action<IssueSourceConfig>) {
        issueSourcesConfig.issueSource(DEFAULT_ISSUE_SOURCE_NAME, action)
    }

    /**
     * Shorthand for declaring the single unnamed issue source of this changelog using a Groovy closure.
     *
     * @param configurationClosure The Groovy closure applied to the new [IssueSourceConfig].
     *
     * @see issueSource
     */
    fun issueSource(
        @DelegatesTo(value = IssueSourceConfig::class, strategy = Closure.DELEGATE_FIRST)
        configurationClosure: Closure<in IssueSourceConfig>,
    ) {
        issueSource { target -> configureGroovy(configurationClosure, target) }
    }

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
    abstract val commitMessageKey: Property<String>

    /**
     * Strategy for formatting annotated tag messages in the changelog output.
     *
     * This strategy determines how annotated tag messages are formatted when included
     * in the generated changelog. Annotated tags in Git can contain messages that
     * provide context about a release or version.
     *
     * @see AnnotatedTagMessageStrategy
     */
    @get:Input
    @get:Optional
    internal abstract val annotatedTagMessageStrategy: Property<AnnotatedTagMessageStrategy>

    /**
     * Strategy for formatting commit messages in the changelog output.
     *
     * This strategy determines how individual commit messages are formatted when included
     * in the generated changelog. It allows customization of message formatting, filtering,
     * and transformation.
     *
     * @see ChangelogMessageStrategy
     */
    @get:Input
    @get:Optional
    internal abstract val changelogMessageStrategy: Property<ChangelogMessageStrategy>

    /**
     * Strategy for generating changelog messages when no changes are detected.
     *
     * This strategy determines the message that appears in the changelog when there are
     * no new commits between the current and previous build tags. It allows customization
     * of the "no changes" message format.
     *
     * @see EmptyChangelogMessageStrategy
     */
    @get:Input
    @get:Optional
    internal abstract val emptyChangelogMessageStrategy: Property<EmptyChangelogMessageStrategy>

    /**
     * Strategy for generating changelog messages when the changelog could not be generated.
     *
     * This strategy determines the message that appears when the changelog generation
     * process fails or cannot produce a valid changelog. It allows customization
     * of the error or fallback message format.
     *
     * @see NotGeneratedChangelogMessageStrategy
     */
    @get:Input
    @get:Optional
    internal abstract val notGeneratedChangelogMessageStrategy: Property<NotGeneratedChangelogMessageStrategy>

    /**
     * Configures the strategy used to format annotated tag messages in the changelog.
     *
     * @param action Supplier that returns the desired [AnnotatedTagMessageStrategy] implementation.
     *
     * @see AnnotatedTagMessageStrategy
     */
    fun annotatedTagMessageStrategy(action: () -> AnnotatedTagMessageStrategy) {
        annotatedTagMessageStrategy.set(action())
    }

    /**
     * Configures the strategy used to format annotated tag messages in the changelog
     * using a Groovy closure.
     *
     * @param strategyClosure Groovy closure that returns the desired [AnnotatedTagMessageStrategy] implementation.
     *
     * @see AnnotatedTagMessageStrategy
     */
    fun annotatedTagMessageStrategy(
        @DelegatesTo(
            value = AnnotatedTagMessageStrategy::class,
            strategy = Closure.DELEGATE_FIRST,
        )
        strategyClosure: Closure<out AnnotatedTagMessageStrategy>,
    ) {
        annotatedTagMessageStrategy.set(strategyClosure.call())
    }

    /**
     * Configures the strategy used to format commit messages in the changelog.
     *
     * @param action Supplier that returns the desired [ChangelogMessageStrategy] implementation.
     *
     * @see ChangelogMessageStrategy
     */
    fun changelogMessageStrategy(action: () -> ChangelogMessageStrategy) {
        changelogMessageStrategy.set(action())
    }

    /**
     * Configures the strategy used to format commit messages in the changelog
     * using a Groovy closure.
     *
     * @param strategyClosure Groovy closure that returns the desired [ChangelogMessageStrategy] implementation.
     *
     * @see ChangelogMessageStrategy
     */
    fun changelogMessageStrategy(
        @DelegatesTo(
            value = ChangelogMessageStrategy::class,
            strategy = Closure.DELEGATE_FIRST,
        )
        strategyClosure: Closure<out ChangelogMessageStrategy>,
    ) {
        changelogMessageStrategy.set(strategyClosure.call())
    }

    /**
     * Configures the strategy used to generate changelog messages when no changes are detected.
     *
     * @param action Supplier that returns the desired [EmptyChangelogMessageStrategy] implementation.
     *
     * @see EmptyChangelogMessageStrategy
     */
    fun emptyChangelogMessageStrategy(action: () -> EmptyChangelogMessageStrategy) {
        emptyChangelogMessageStrategy.set(action())
    }

    /**
     * Configures the strategy used to generate changelog messages when no changes are detected
     * using a Groovy closure.
     *
     * @param strategyClosure Groovy closure that returns the desired [EmptyChangelogMessageStrategy] implementation.
     *
     * @see EmptyChangelogMessageStrategy
     */
    fun emptyChangelogMessageStrategy(
        @DelegatesTo(
            value = EmptyChangelogMessageStrategy::class,
            strategy = Closure.DELEGATE_FIRST,
        )
        strategyClosure: Closure<out EmptyChangelogMessageStrategy>,
    ) {
        emptyChangelogMessageStrategy.set(strategyClosure.call())
    }

    /**
     * Configures the strategy used to generate changelog messages when the changelog could not be generated.
     *
     * @param action Supplier that returns the desired [NotGeneratedChangelogMessageStrategy] implementation.
     *
     * @see NotGeneratedChangelogMessageStrategy
     */
    fun notGeneratedChangelogMessageStrategy(action: () -> NotGeneratedChangelogMessageStrategy) {
        notGeneratedChangelogMessageStrategy.set(action())
    }

    /**
     * Configures the strategy used to generate changelog messages when the changelog could not be generated.
     *
     * This overload accepts a Groovy [Closure] for compatibility with Groovy-based build scripts.
     *
     * @param strategyClosure Closure that returns the desired [NotGeneratedChangelogMessageStrategy] implementation.
     *
     * @see NotGeneratedChangelogMessageStrategy
     */
    fun notGeneratedChangelogMessageStrategy(
        @DelegatesTo(
            value = NotGeneratedChangelogMessageStrategy::class,
            strategy = Closure.DELEGATE_FIRST,
        )
        strategyClosure: Closure<out NotGeneratedChangelogMessageStrategy>,
    ) {
        notGeneratedChangelogMessageStrategy.set(strategyClosure.call())
    }

    override fun inheritFrom(common: ChangelogConfig) {
        issueSourcesConfig.inheritFrom(common.issueSourcesConfig)
        commitMessageKey.convention(common.commitMessageKey)
        annotatedTagMessageStrategy.convention(common.annotatedTagMessageStrategy)
        changelogMessageStrategy.convention(common.changelogMessageStrategy)
        emptyChangelogMessageStrategy.convention(common.emptyChangelogMessageStrategy)
        notGeneratedChangelogMessageStrategy.convention(common.notGeneratedChangelogMessageStrategy)
    }
}

private const val DEFAULT_ISSUE_SOURCE_NAME = "default"
