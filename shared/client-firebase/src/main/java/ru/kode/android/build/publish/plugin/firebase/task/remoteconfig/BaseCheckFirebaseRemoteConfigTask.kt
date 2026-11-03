package ru.kode.android.build.publish.plugin.firebase.task.remoteconfig

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.language.base.plugins.LifecycleBasePlugin
import ru.kode.android.build.publish.plugin.core.entity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.core.git.GitCommandExecutor
import ru.kode.android.build.publish.plugin.firebase.config.FirebaseRemoteConfigConfig
import ru.kode.android.build.publish.plugin.firebase.config.KeySourceSpec
import ru.kode.android.build.publish.plugin.firebase.controller.FirebaseRemoteConfigControllerFactory
import ru.kode.android.build.publish.plugin.firebase.controller.audit
import ru.kode.android.build.publish.plugin.firebase.controller.writeReports
import ru.kode.android.build.publish.plugin.firebase.messages.remoteConfigFailedMessage
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.AuditRequest
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.FindingType
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.Severity
import ru.kode.android.gradle.commons.logger.LoggerService

/**
 * Inputs and action shared by the Remote Config audit tasks of the Firebase and sender plugins, which differ
 * only in how they find the release tags and the git repository.
 *
 * Writes `report.md` / `report.json` / `report.sarif` and fails when a finding has [Severity.Fail].
 */
abstract class BaseCheckFirebaseRemoteConfigTask : DefaultTask() {
    init {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val serviceCredentialsFile: RegularFileProperty

    @get:Input
    abstract val projectId: Property<String>

    @get:Input
    @get:Optional
    abstract val apiBaseUrl: Property<String>

    @get:Input
    abstract val keySources: ListProperty<KeySourceSpec>

    @get:Input
    abstract val usageSearchIncludes: ListProperty<String>

    @get:Input
    abstract val dynamicKeyPrefixes: SetProperty<String>

    @get:Input
    abstract val ignoreKeys: SetProperty<String>

    @get:Input
    abstract val severities: MapProperty<FindingType, Severity>

    @get:Input
    abstract val flagLifetimes: MapProperty<String, Int>

    @get:Input
    abstract val inAppDefaultsIncludes: ListProperty<String>

    /**
     * `versionName` of the audited build, used for `app.version` conditions.
     */
    @get:Input
    @get:Optional
    abstract val appVersion: Property<String>

    /**
     * `versionCode` of the audited build, used for `app.build` conditions.
     */
    @get:Input
    @get:Optional
    abstract val appBuild: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val loggerService: Property<LoggerService>

    /**
     * Wires the inputs from the DSL [config].
     */
    fun configureFrom(config: FirebaseRemoteConfigConfig) {
        serviceCredentialsFile.set(config.serviceCredentialsFile)
        projectId.set(config.projectId)
        apiBaseUrl.set(config.apiBaseUrl)
        keySources.set(config.keySources)
        usageSearchIncludes.set(config.usageSearchIncludes)
        dynamicKeyPrefixes.set(config.dynamicKeyPrefixes)
        ignoreKeys.set(config.ignoreKeys)
        severities.set(config.severities)
        flagLifetimes.set(config.flagLifetimes)
        inAppDefaultsIncludes.set(config.inAppDefaultsIncludes)
    }

    protected fun audit(
        snapshot: BuildTagSnapshot,
        git: GitCommandExecutor,
        appVersion: String? = this.appVersion.orNull,
        appBuild: String? = this.appBuild.orNull,
    ) {
        val logger = loggerService.get().logger
        val controller =
            FirebaseRemoteConfigControllerFactory.build(serviceCredentialsFile.get().asFile, logger, apiBaseUrl.orNull)
        val request =
            AuditRequest(
                projectId = projectId.get(),
                snapshot = snapshot,
                keySources = keySources.get(),
                usageSearchIncludes = usageSearchIncludes.get(),
                dynamicKeyPrefixes = dynamicKeyPrefixes.get(),
                ignoreKeys = ignoreKeys.get(),
                severities = severities.get(),
                appVersion = appVersion,
                appBuild = appBuild,
                flagLifetimes = flagLifetimes.get(),
                inAppDefaultsIncludes = inAppDefaultsIncludes.get(),
            )
        val audit = controller.audit(request, git, logger)
        val outputDir = outputDirectory.get().asFile
        logger.quiet(audit.writeReports(outputDir))
        logger.quiet("Remote Config report: ${outputDir.resolve("report.md")}")

        val failures = audit.items.filter { it.severity == Severity.Fail }
        if (failures.isNotEmpty()) {
            throw GradleException(remoteConfigFailedMessage(failures.map { "${it.finding.type}: ${it.finding}" }))
        }
    }
}
