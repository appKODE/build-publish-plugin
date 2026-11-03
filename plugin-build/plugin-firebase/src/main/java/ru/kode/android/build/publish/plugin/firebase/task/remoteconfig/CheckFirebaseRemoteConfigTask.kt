package ru.kode.android.build.publish.plugin.firebase.task.remoteconfig

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.foundation.service.git.GitExecutorService

/**
 * Compares the Remote Config keys declared in code at the current release tag of the variant (and the previous
 * one) with the live Firebase Remote Config template, see [BaseCheckFirebaseRemoteConfigTask].
 */
@UntrackedTask(because = "The Remote Config template changes outside of the build")
abstract class CheckFirebaseRemoteConfigTask : BaseCheckFirebaseRemoteConfigTask() {
    init {
        description = "Audits Firebase Remote Config against the keys declared in code for the last release tag"
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val buildTagSnapshotFile: RegularFileProperty

    @get:Internal
    abstract val gitExecutorService: Property<GitExecutorService>

    @TaskAction
    fun check() {
        audit(fromJson(buildTagSnapshotFile.get().asFile), gitExecutorService.get().commandExecutor)
    }
}
