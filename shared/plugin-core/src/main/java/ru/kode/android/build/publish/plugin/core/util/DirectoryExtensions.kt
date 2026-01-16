package ru.kode.android.build.publish.plugin.core.util

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

fun Project.changelogFileProvider(variantName: String): Provider<RegularFile> {
    return this.layout.buildDirectory.file("changelog-$variantName.txt")
}

fun Project.tagBuildSnapshotFileProvider(variantName: String): Provider<RegularFile> {
    return project.layout.buildDirectory
        .file("tag-build-snapshot-$variantName.json")
}

fun Project.versionCodeFileProvider(variantName: String): Provider<RegularFile> {
    return project.layout.buildDirectory
        .file("version-code-$variantName.txt")
}

fun Project.apkOutputFileNameProvider(variantName: String): Provider<RegularFile> {
    return project.layout.buildDirectory
        .file("apk-output-filename-$variantName.txt")
}

fun Project.versionNameProvider(variantName: String): Provider<RegularFile> {
    return project.layout.buildDirectory
        .file("version-name-$variantName.txt")
}
