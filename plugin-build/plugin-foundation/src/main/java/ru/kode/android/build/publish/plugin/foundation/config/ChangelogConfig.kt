package ru.kode.android.build.publish.plugin.foundation.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import ru.kode.android.build.publish.plugin.core.strategy.AnnotatedTagMessageStrategy
import ru.kode.android.build.publish.plugin.core.strategy.ChangelogMessageStrategy
import ru.kode.android.build.publish.plugin.core.strategy.EmptyChangelogMessageStrategy
import ru.kode.android.build.publish.plugin.core.strategy.NotGeneratedChangelogMessageStrategy

/**
 * Configuration interface for changelog generation settings.
 *
 * This interface defines the configuration options for generating changelogs from
 * Git commit history. It allows customization of how issue tracking is integrated
 * and which commits should be included in the changelog.
 */
abstract class ChangelogConfig {
    abstract val name: String

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
    abstract val issueNumberPattern: Property<String>

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
    abstract val issueUrlPrefix: Property<String>

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
}
