package ru.kode.android.build.publish.plugin.core.util

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

internal const val CHANGELOG_FILENAME = "changelog.txt"

fun Project.changelogDirectory(): Provider<RegularFile> {
    return this.layout.buildDirectory.file(CHANGELOG_FILENAME)
}
