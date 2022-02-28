package ru.kode.android.firebase.publish.plugin.configuration

import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import org.gradle.api.Project
import ru.kode.android.firebase.publish.plugin.command.LinuxShellCommandExecutor
import ru.kode.android.firebase.publish.plugin.command.ShellCommandExecutor
import ru.kode.android.firebase.publish.plugin.util.Changelog

fun AppDistributionExtension.configure(
    project: Project,
    distributionServiceKey: String,
    commitMessageKey: String,
    buildVariants: Set<String>,
    groups: Set<String>
) {
    val commandExecutor = LinuxShellCommandExecutor(project)
    serviceCredentialsFile = System.getenv(distributionServiceKey).orEmpty()
    releaseNotes = buildChangelog(project, commandExecutor, commitMessageKey, buildVariants)
    this.groups = groups.joinToString(",")
}

private fun buildChangelog(
    project: Project,
    commandExecutor: ShellCommandExecutor,
    messageKey: String,
    buildVariants: Set<String>,
): String {
    return Changelog(commandExecutor, project.logger, messageKey, buildVariants)
        .buildForRecentBuildTag()
        .also {
            if (it.isNullOrBlank()) {
                project.logger.warn("App Distribution changelog is empty")
            }
        }
        .orEmpty()
}
