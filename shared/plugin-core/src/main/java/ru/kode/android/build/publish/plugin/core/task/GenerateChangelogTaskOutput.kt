package ru.kode.android.build.publish.plugin.core.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option
import ru.kode.android.build.publish.plugin.core.issue.IssueResolver

/**
 * Represents the output of the [GenerateChangelogTask].
 */
abstract class GenerateChangelogTaskOutput : DefaultTask() {
    /**
     * The output file where the generated changelog will be saved.
     *
     * @see RegularFileProperty
     */
    @get:OutputFile
    @get:Option(
        option = "changelogFile",
        description = "The output file where the generated changelog will be saved",
    )
    abstract val changelogFile: RegularFileProperty

    /**
     * Resolvers contributed by provider plugins (Jira, ClickUp, …) that turn a `CLOSES`/`FIXES` issue
     * reference into its tracker title. Each provider plugin appends its own resolver via
     * `issueResolvers.add(...)`; the task tries them in order. Empty when no provider is applied, in which
     * case reference resolution is skipped and changelog generation is unchanged.
     *
     * @see IssueResolver
     */
    @get:Internal
    abstract val issueResolvers: ListProperty<IssueResolver>
}
