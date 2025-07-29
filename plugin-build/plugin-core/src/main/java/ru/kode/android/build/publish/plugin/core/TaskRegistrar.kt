package ru.kode.android.build.publish.plugin.core

import org.ajoberstar.grgit.gradle.GrgitService
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.enity.TaskOutput

interface TaskRegistrar {
    fun registerTasks(
        project: Project,
        buildVariant: BuildVariant,
        changelogFile: Provider<RegularFile>,
        apkOutputFileName: String,
        bundleFile: Provider<RegularFile>,
        grgitService: GrgitService
    ): TaskOutput
}
