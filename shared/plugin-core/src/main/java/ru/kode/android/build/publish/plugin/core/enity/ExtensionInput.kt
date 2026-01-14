package ru.kode.android.build.publish.plugin.core.enity

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

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
         * Regex pattern to extract issue numbers from commit messages
         */
        val issueNumberPattern: Provider<String>,
        /**
         * Base URL prefix for generating issue links
         */
        val issueUrlPrefix: Provider<String>,
        /**
         * Key used to identify the commit message in the Git log
         */
        val commitMessageKey: Provider<String>,
        /**
         * Provider for the changelog file
         */
        val file: Provider<RegularFile>,
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
        val lastBuildTagFile: Provider<RegularFile>,
        /**
         * Name of the changelog file name
         */
        val changelogFileName: Provider<RegularFile>,
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
