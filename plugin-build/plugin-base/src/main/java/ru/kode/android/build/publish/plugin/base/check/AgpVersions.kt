package ru.kode.android.build.publish.plugin.base.check

import com.android.build.gradle.AppPlugin
import org.gradle.util.internal.VersionNumber
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException

internal object AgpVersions {
    val CURRENT: VersionNumber = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION).baseVersion
    val VERSION_7_0_4: VersionNumber = VersionNumber.parse("7.0.4")
}

@Suppress("ThrowsCount") // block to throws exceptions on apply
internal fun Project.stopExecutionIfNotSupported() {
    if (AgpVersions.CURRENT < AgpVersions.VERSION_7_0_4) {
        throw StopExecutionException(
            "Must only be used with with Android Gradle Plugin >= 7.4 ",
        )
    }
    if (!plugins.hasPlugin(AppPlugin::class.java)) {
        throw StopExecutionException(
            "Must only be used with Android application projects." +
                " Please apply the 'com.android.application' plugin.",
        )
    }
}