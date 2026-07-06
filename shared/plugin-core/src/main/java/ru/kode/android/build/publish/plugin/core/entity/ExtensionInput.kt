package ru.kode.android.build.publish.plugin.core.entity

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.task.GenerateChangelogTaskOutput
import ru.kode.android.build.publish.plugin.core.task.GetLastTagSnapshotTaskOutput

/**
 * Container class that holds all the input data required for build and publish operations.
 */
data class ExtensionInput(
    /**
     * Configuration and data related to the changelog
     */
    val changelog: Changelog,
    /**
     * Build output files and metadata
     */
    val output: Output,
    /**
     * Information about the current build variant
     */
    val buildVariant: BuildVariant,
) {
    /**
     * Configuration and data related to the changelog generation and processing.
     */
    data class Changelog(
        /**
         * Issue-tracker sources: each pairs an extraction regex with the URL its keys link to.
         * Consumers union the patterns to extract issue keys and resolve links per matching source.
         */
        val issueSources: Provider<List<IssueSource>>,
        /**
         * Key used to identify the commit message in the Git log
         */
        val commitMessageKey: Provider<String>,
        /**
         * Provider for the changelog file
         */
        val fileProvider: TaskProvider<out GenerateChangelogTaskOutput>,
    )

    /**
     * Information about the build outputs and versioning.
     */
    data class Output(
        /**
         * Base name used for generated output files
         */
        val baseFileName: Provider<String>,
        /**
         * Pattern used for creating Git tags for builds
         */
        val buildTagPattern: Provider<String>,
        /**
         * File that stores the last build tag
         */
        val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
        /**
         * Provider for the APK file location
         */
        val apkFile: Provider<RegularFile>,
        /**
         * Provider for the AAB bundle file location
         */
        val bundleFile: Provider<RegularFile>,
    )
}
