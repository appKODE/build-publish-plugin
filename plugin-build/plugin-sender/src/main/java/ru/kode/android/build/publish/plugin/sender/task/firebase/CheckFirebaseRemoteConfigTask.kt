package ru.kode.android.build.publish.plugin.sender.task.firebase

import org.ajoberstar.grgit.Grgit
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import ru.kode.android.build.publish.plugin.core.git.GitCommandExecutor
import ru.kode.android.build.publish.plugin.firebase.task.remoteconfig.BaseCheckFirebaseRemoteConfigTask

/**
 * Audits Firebase Remote Config for the last two git tags matching [buildTagPattern], without an Android build;
 * see [BaseCheckFirebaseRemoteConfigTask].
 */
@UntrackedTask(because = "The Remote Config template changes outside of the build")
abstract class CheckFirebaseRemoteConfigTask : BaseCheckFirebaseRemoteConfigTask() {
    init {
        description = "Audits Firebase Remote Config against the keys declared in code for the last release tag"
    }

    /**
     * Regular expression of release tags, e.g. `v\d+\.\d+\.\d+`; the last number is the build number.
     */
    @get:Input
    abstract val buildTagPattern: Property<String>

    @get:Internal
    abstract val repositoryDirectory: DirectoryProperty

    @TaskAction
    fun check() {
        val pattern = buildTagPattern.get()
        Grgit.open(mapOf("dir" to repositoryDirectory.get().asFile)).use { grgit ->
            val logger = loggerService.get().logger
            val git = GitCommandExecutor(grgit, logger)
            // Tag.Build requires the tag name to contain the variant; there is no variant here.
            val snapshot =
                git.findSnapshot(buildVariant = "", buildTagRegex = Regex(pattern), ciCommitTag = null)
                    ?: throw GradleException("No git tag matches the Remote Config buildTagPattern '$pattern'")
            val current = snapshot.current
            val version = appVersion.orNull ?: "${current.buildVersion}.${current.buildNumber}"
            val build = appBuild.orNull ?: current.buildNumber.toString()
            if (!appVersion.isPresent || !appBuild.isPresent) {
                logger.info("Remote Config audit uses app.version=$version, app.build=$build from tag ${current.name}")
            }
            audit(snapshot = snapshot, git = git, appVersion = version, appBuild = build)
        }
    }
}
