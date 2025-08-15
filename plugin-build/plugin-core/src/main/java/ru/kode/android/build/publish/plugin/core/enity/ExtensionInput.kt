package ru.kode.android.build.publish.plugin.core.enity

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

data class ExtensionInput(
    val changelog: Changelog,
    val output: Output,
    val buildVariant: BuildVariant,
) {
    data class Changelog(
        val issueNumberPattern: Provider<String>,
        val issueUrlPrefix: Provider<String>,
        val commitMessageKey: Provider<String>,
        val file: Provider<RegularFile>,
    )

    data class Output(
        val versionName: Provider<String>?,
        val versionCode: Provider<Int>?,
        val baseFileName: Provider<String>,
        val buildTagPattern: Provider<String>,
        val lastBuildTagFile: Provider<RegularFile>,
        val apkFileName: Provider<String>,
        val apkFile: Provider<RegularFile>,
        val bundleFile: Provider<RegularFile>,
    )
}
