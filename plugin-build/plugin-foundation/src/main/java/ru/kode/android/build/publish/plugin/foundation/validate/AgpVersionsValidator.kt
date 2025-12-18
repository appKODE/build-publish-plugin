package ru.kode.android.build.publish.plugin.foundation.validate

import com.android.build.gradle.AppPlugin
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import org.gradle.util.internal.VersionNumber
import ru.kode.android.build.publish.plugin.foundation.messages.mustBeUsedWithAndroidMessage
import ru.kode.android.build.publish.plugin.foundation.messages.mustBeUsedWithVersionMessage

@Suppress("ThrowsCount") // block to throws exceptions on apply
internal fun Project.stopExecutionIfNotSupported() {
    if (AgpVersions.CURRENT < AgpVersions.VERSION_7_0_4) {
        throw StopExecutionException(mustBeUsedWithVersionMessage())
    }
    if (!plugins.hasPlugin(AppPlugin::class.java)) {
        throw StopExecutionException(mustBeUsedWithAndroidMessage())
    }
}

internal object AgpVersions {
    val CURRENT: VersionNumber = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION).baseVersion
    val VERSION_7_0_4: VersionNumber = VersionNumber.parse("7.0.4")
}
