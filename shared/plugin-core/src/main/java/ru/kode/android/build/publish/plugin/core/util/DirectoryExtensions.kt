package ru.kode.android.build.publish.plugin.core.util

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

fun Project.changelogFileProvider(variantName: String): Provider<RegularFile> {
    return this.layout.buildDirectory.file("changelog-$variantName.txt")
}

fun Project.tagBuildFileProvider(variantName: String): Provider<RegularFile> {
    return project.layout.buildDirectory
        .file("tag-build-$variantName.json")
}
