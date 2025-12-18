package ru.kode.android.build.publish.plugin.foundation.util

import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.foundation.messages.noOutputVariantMessage

internal fun Project.mapToOutputApkFile(
    buildVariant: BuildVariant,
    fileName: String,
): Provider<RegularFile> {
    return tasks.withType(PackageAndroidArtifact::class.java)
        .firstOrNull { it.variantName == buildVariant.name }
        ?.outputDirectory
        ?.map { directory -> directory.file(fileName) }
        ?: throw GradleException(noOutputVariantMessage(buildVariant))
}

