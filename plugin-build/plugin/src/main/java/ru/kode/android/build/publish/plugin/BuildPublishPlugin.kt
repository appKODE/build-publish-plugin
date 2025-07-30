@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.impl.VariantOutputImpl
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.tasks.PackageAndroidArtifact
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import org.ajoberstar.grgit.gradle.GrgitService
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.gradle.GrgitServicePlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskContainer
import org.gradle.util.internal.VersionNumber
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterDistributionTaskParams
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterTasksRegistrar
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpAutomationTaskParams
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpTasksRegistrar
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceDistributionTaskParams
import ru.kode.android.build.publish.plugin.confluence.task.ConfluenceTasksRegistrar
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.extension.BuildPublishExtension
import ru.kode.android.build.publish.plugin.extension.EXTENSION_NAME
import ru.kode.android.build.publish.plugin.extension.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.firebase.BuildPublishFirebasePlugin
import ru.kode.android.build.publish.plugin.jira.task.JiraAutomationTaskParams
import ru.kode.android.build.publish.plugin.jira.task.JiraTasksRegistrar
import ru.kode.android.build.publish.plugin.play.task.PlayTaskParams
import ru.kode.android.build.publish.plugin.play.task.PlayTasksRegistrar
import ru.kode.android.build.publish.plugin.slack.task.SlackChangelogTaskParams
import ru.kode.android.build.publish.plugin.slack.task.SlackDistributionTasksParams
import ru.kode.android.build.publish.plugin.slack.task.SlackTasksRegistrar
import ru.kode.android.build.publish.plugin.task.changelog.GenerateChangelogTask
import ru.kode.android.build.publish.plugin.task.tag.GetLastTagTask
import ru.kode.android.build.publish.plugin.task.tag.PrintLastIncreasedTag
import ru.kode.android.build.publish.plugin.telegram.task.TelegramChangelogTaskParams
import ru.kode.android.build.publish.plugin.telegram.task.TelegramDistributionTasksParams
import ru.kode.android.build.publish.plugin.telegram.task.TelegramTasksRegistrar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal const val GENERATE_CHANGELOG_TASK_PREFIX = "generateChangelog"
internal const val PRINT_LAST_INCREASED_TAG_TASK_PREFIX = "printLastIncreasedTag"
internal const val GET_LAST_TAG_TASK_PREFIX = "getLastTag"
internal const val DEFAULT_BUILD_VERSION = "v0.0.1"
internal const val DEFAULT_VERSION_NAME = "$DEFAULT_BUILD_VERSION-dev"
internal const val DEFAULT_VERSION_CODE = 1
internal const val DEFAULT_BASE_FILE_NAME = "dev-build"
internal const val CHANGELOG_FILENAME = "changelog.txt"
internal const val DEFAULT_CONTAINER_NAME = "default"

internal object AgpVersions {
    val CURRENT: VersionNumber = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION).baseVersion
    val VERSION_7_0_4: VersionNumber = VersionNumber.parse("7.0.4")
}

abstract class BuildPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.stopExecutionIfNotSupported()
        project.pluginManager.apply(GrgitServicePlugin::class.java)

        val buildPublishExtension = project.extensions
            .create(EXTENSION_NAME, BuildPublishExtension::class.java)
        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)
        val changelogFileProvider = project.layout.buildDirectory.file(CHANGELOG_FILENAME)
        val grgitService = project.extensions
            .getByType(GrgitServiceExtension::class.java)
            .service

        // TODO: Decide how to handle such extensions? Maybe it will be possible to do it dynamically
        val firebaseDistributionExtensions = buildPublishExtension
            .firebaseDistribution
            .findByName(DEFAULT_CONTAINER_NAME)

        if (firebaseDistributionExtensions != null) {
            project.pluginManager.apply(BuildPublishFirebasePlugin::class.java)
        }

        androidExtension.onVariants(
            callback = { variant ->
                val variantOutput = variant.outputs
                    .find { it is VariantOutputImpl && it.fullName == variant.name }
                    as? VariantOutputImpl

                if (variantOutput != null) {
                    val buildVariant = BuildVariant(variant.name, variant.flavorName, variant.buildType)
                    val apkOutputFileName = variantOutput.outputFileName.get()

                    val bundleFile = variant.artifacts.get(SingleArtifact.BUNDLE)
                    val outputProviders = project.registerVariantTasks(
                        buildPublishExtension,
                        buildVariant,
                        apkOutputFileName,
                        grgitService,
                    )

                    project.tasks.registerPrintLastIncreasedTagTask(
                        buildVariant,
                        outputProviders.tagBuildProvider,
                    )

                    val changelogConfig =
                        with(buildPublishExtension.changelog) {
                            findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
                        }

                    if (changelogConfig != null) {
                        val apkOutputFileProvider = outputProviders.apkOutputFileName.flatMap { fileName ->
                            project.mapToOutputApkFile(buildVariant, fileName)
                        }
                        project.tasks.registerChangelogDependentTasks(
                            changelogConfig,
                            buildPublishExtension,
                            buildVariant,
                            changelogFileProvider,
                            outputProviders,
                            grgitService,
                            apkOutputFileProvider,
                            bundleFile
                        )
                    }

                    if (outputProviders.versionCode != null) {
                        variantOutput.versionCode.set(outputProviders.versionCode)
                    }
                    if (outputProviders.versionName != null) {
                        variantOutput.versionName.set(outputProviders.versionName)
                    }
                    variantOutput.outputFileName.set(outputProviders.apkOutputFileName)
                }
            },
        )
        androidExtension.finalizeDsl {
            val outputConfig = with(buildPublishExtension.output) {
                getByName(DEFAULT_CONTAINER_NAME)
            }
            val useDefaultVersionsAsFallback = outputConfig
                .useDefaultVersionsAsFallback
                .getOrElse(true)

            project.plugins.all { plugin ->
                when (plugin) {
                    is AppPlugin -> {
                        if (useDefaultVersionsAsFallback) {
                            val appExtension = project.extensions.getByType(AppExtension::class.java)
                            appExtension.configure()
                        }
                    }
                }
            }
        }
    }

    private fun TaskContainer.registerChangelogDependentTasks(
        changelogConfig: ChangelogConfig,
        buildPublishExtension: BuildPublishExtension,
        buildVariant: BuildVariant,
        changelogFileProvider: Provider<RegularFile>,
        outputProviders: OutputProviders,
        grgitService: Property<GrgitService>,
        apkOutputFileProvider: Provider<RegularFile>,
        bundleFile: Provider<RegularFile>
    ) {
        val outputConfig =
            with(buildPublishExtension.output) {
                findByName(buildVariant.name) ?: getByName(DEFAULT_CONTAINER_NAME)
            }

        val generateChangelogFileProvider =
            registerGenerateChangelogTask(
                changelogConfig.commitMessageKey,
                outputConfig.buildTagPattern,
                buildVariant,
                changelogFileProvider,
                outputProviders.tagBuildProvider,
                grgitService,
            )

        with(buildPublishExtension.telegram) {
            findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
        }?.apply {
            TelegramTasksRegistrar.registerChangelogTask(
                project = this@registerChangelogDependentTasks,
                config = this,
                params = TelegramChangelogTaskParams(
                    outputConfig.baseFileName,
                    changelogConfig.issueNumberPattern,
                    changelogConfig.issueUrlPrefix,
                    buildVariant,
                    generateChangelogFileProvider,
                    outputProviders.tagBuildProvider,
                )
            )
            TelegramTasksRegistrar.registerDistributionTask(
                project = this@registerChangelogDependentTasks,
                config = this,
                params = TelegramDistributionTasksParams(
                    outputConfig.baseFileName,
                    buildVariant,
                    outputProviders.tagBuildProvider,
                    apkOutputFileProvider,
                )
            )
        }

        with(buildPublishExtension.confluence) {
            findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
        }?.apply {
            ConfluenceTasksRegistrar.registerDistributionTask(
                project = this@registerChangelogDependentTasks,
                config = this,
                params = ConfluenceDistributionTaskParams(
                    buildVariant = buildVariant,
                    apkOutputFileProvider = apkOutputFileProvider,
                )
            )
        }
        with(buildPublishExtension.slack) {
            findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
        }?.apply {
            SlackTasksRegistrar.registerChangelogTask(
                project = this@registerChangelogDependentTasks,
                config = this,
                params = SlackChangelogTaskParams(
                    outputConfig.baseFileName,
                    changelogConfig.issueNumberPattern,
                    changelogConfig.issueUrlPrefix,
                    buildVariant,
                    generateChangelogFileProvider,
                    outputProviders.tagBuildProvider,
                )
            )
            SlackTasksRegistrar.registerDistributionTask(
                project = this@registerChangelogDependentTasks,
                config = this,
                params = SlackDistributionTasksParams(
                    outputConfig.baseFileName,
                    buildVariant,
                    outputProviders.tagBuildProvider,
                    apkOutputFileProvider,
                )
            )
        }

        with(buildPublishExtension.appCenterDistribution) {
            findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
        }?.apply {
            AppCenterTasksRegistrar.registerDistributionTask(
                project = this@registerChangelogDependentTasks,
                config = this,
                params = AppCenterDistributionTaskParams(
                    buildVariant = buildVariant,
                    changelogFileProvider = generateChangelogFileProvider,
                    apkOutputFileProvider = apkOutputFileProvider,
                    tagBuildProvider = outputProviders.tagBuildProvider,
                    baseFileName = outputConfig.baseFileName,
                )
            )
        }

        with(buildPublishExtension.play) {
            findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
        }?.apply {
            PlayTasksRegistrar.registerDistributionTask(
                project = this@registerChangelogDependentTasks,
                config = this,
                params = PlayTaskParams(
                    buildVariant = buildVariant,
                    bundleOutputFileProvider = bundleFile,
                    tagBuildProvider = outputProviders.tagBuildProvider,
                )
            )
        }

        with(buildPublishExtension.jira) {
            findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
        }?.apply {
            JiraTasksRegistrar.registerAutomationTask(
                project = this@registerChangelogDependentTasks,
                config = this,
                params = JiraAutomationTaskParams(
                    buildVariant = buildVariant,
                    issueNumberPattern = changelogConfig.issueNumberPattern,
                    changelogFileProvider = changelogFileProvider,
                    tagBuildProvider = outputProviders.tagBuildProvider,
                )
            )
        }

        with(buildPublishExtension.clickUp) {
            findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
        }?.apply {
            ClickUpTasksRegistrar.registerAutomationTask(
                project = this@registerChangelogDependentTasks,
                config = this,
                params = ClickUpAutomationTaskParams(
                    buildVariant = buildVariant,
                    issueNumberPattern = changelogConfig.issueNumberPattern,
                    changelogFileProvider = changelogFileProvider,
                    tagBuildProvider = outputProviders.tagBuildProvider,
                )
            )
        }
    }

    private fun Project.registerVariantTasks(
        buildPublishExtension: BuildPublishExtension,
        buildVariant: BuildVariant,
        apkOutputFileName: String,
        grgitService: Provider<GrgitService>,
    ): OutputProviders {
        val outputConfig =
            with(buildPublishExtension.output) {
                findByName(buildVariant.name) ?: getByName(DEFAULT_CONTAINER_NAME)
            }
        val tagBuildProvider =
            registerGetLastTagTask(
                buildVariant,
                outputConfig.buildTagPattern,
                grgitService,
            )
        val useVersionsFromTag = outputConfig.useVersionsFromTag.getOrElse(true)
        val useDefaultVersionsAsFallback =
            outputConfig.useDefaultVersionsAsFallback.getOrElse(true)
        val versionCodeProvider =
            when {
                useVersionsFromTag -> tagBuildProvider.map(::mapToVersionCode)
                useDefaultVersionsAsFallback -> project.provider { DEFAULT_VERSION_CODE }
                else -> null
            }
        val apkOutputFileNameProvider =
            if (useVersionsFromTag) {
                outputConfig.baseFileName.zip(tagBuildProvider) { baseFileName, tagBuildFile ->
                    mapToOutputApkFileName(tagBuildFile, apkOutputFileName, baseFileName)
                }
            } else {
                outputConfig.baseFileName.map { baseFileName ->
                    createDefaultOutputFileName(baseFileName, apkOutputFileName)
                }
            }
        val versionNameProvider =
            when {
                useVersionsFromTag -> {
                    tagBuildProvider.map { tagBuildFile ->
                        mapToVersionName(tagBuildFile, buildVariant)
                    }
                }

                useDefaultVersionsAsFallback -> project.provider { DEFAULT_VERSION_NAME }
                else -> null
            }

        return OutputProviders(
            versionName = versionNameProvider,
            versionCode = versionCodeProvider,
            apkOutputFileName = apkOutputFileNameProvider,
            tagBuildProvider = tagBuildProvider
        )
    }

    private fun Project.registerGetLastTagTask(
        buildVariant: BuildVariant,
        buildTagPattern: Provider<String>,
        grgitService: Provider<GrgitService>,
    ): Provider<RegularFile> {
        val tagBuildFile =
            project.layout.buildDirectory
                .file("tag-build-${buildVariant.name}.json")
        return tasks.register(
            "$GET_LAST_TAG_TASK_PREFIX${buildVariant.capitalizedName()}",
            GetLastTagTask::class.java,
        ) { task ->
            task.tagBuildFile.set(tagBuildFile)
            task.buildVariant.set(buildVariant.name)
            task.buildTagPattern.set(buildTagPattern)
            task.getGrgitService().set(grgitService)
        }.flatMap { it.tagBuildFile }
    }

    private fun TaskContainer.registerPrintLastIncreasedTagTask(
        buildVariant: BuildVariant,
        tagBuildProvider: Provider<RegularFile>,
    ) {
        register(
            "$PRINT_LAST_INCREASED_TAG_TASK_PREFIX${buildVariant.capitalizedName()}",
            PrintLastIncreasedTag::class.java,
        ) { task ->
            task.tagBuildFile.set(tagBuildProvider)
        }
    }

    private fun TaskContainer.registerGenerateChangelogTask(
        commitMessageKey: Provider<String>,
        buildTagPattern: Provider<String>,
        buildVariant: BuildVariant,
        changelogFile: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>,
        grgitService: Provider<GrgitService>,
    ): Provider<RegularFile> {
        return register(
            "$GENERATE_CHANGELOG_TASK_PREFIX${buildVariant.capitalizedName()}",
            GenerateChangelogTask::class.java,
        ) {
            it.commitMessageKey.set(commitMessageKey)
            it.buildTagPattern.set(buildTagPattern)
            it.changelogFile.set(changelogFile)
            it.tagBuildFile.set(tagBuildProvider)
            it.getGrgitService().set(grgitService)
        }.flatMap { it.changelogFile }
    }
}

@Suppress("ThrowsCount") // block to throws exceptions on apply
private fun Project.stopExecutionIfNotSupported() {
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

private fun AppExtension.configure() {
    defaultConfig {
        it.versionCode = DEFAULT_VERSION_CODE
        it.versionName = DEFAULT_VERSION_NAME
    }
}

private data class OutputProviders(
    val versionName: Provider<String>?,
    val versionCode: Provider<Int>?,
    val apkOutputFileName: Provider<String>,
    val tagBuildProvider: Provider<RegularFile>,
)

private fun mapToVersionCode(tagBuildFile: RegularFile): Int {
    val file = tagBuildFile.asFile
    return if (file.exists()) {
        fromJson(file).buildNumber
    } else {
        DEFAULT_VERSION_CODE
    }
}

private fun mapToOutputApkFileName(
    tagBuildFile: RegularFile,
    outputFileName: String,
    baseFileName: String?,
): String {
    val file = tagBuildFile.asFile
    val formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"))
    return if (file.exists() && outputFileName.endsWith(".apk")) {
        val tagBuild = fromJson(file)
        val versionName = tagBuild.buildVariant
        val versionCode = tagBuild.buildNumber
        "$baseFileName-$versionName-vc$versionCode-$formattedDate.apk"
    } else if (!file.exists() && outputFileName.endsWith(".apk")) {
        "$baseFileName-$formattedDate.apk"
    } else {
        createDefaultOutputFileName(baseFileName, outputFileName)
    }
}

private fun createDefaultOutputFileName(
    baseFileName: String?,
    outputFileName: String,
): String {
    return (baseFileName ?: DEFAULT_BASE_FILE_NAME)
        .let { "$it.${outputFileName.split(".").last()}" }
}

private fun mapToVersionName(
    tagBuildFile: RegularFile,
    buildVariant: BuildVariant,
): String {
    val file = tagBuildFile.asFile
    return if (file.exists()) {
        fromJson(tagBuildFile.asFile).name
    } else {
        "$DEFAULT_BUILD_VERSION-${buildVariant.name}"
    }
}

private fun Project.mapToOutputApkFile(
    buildVariant: BuildVariant,
    fileName: String,
): Provider<RegularFile> {
    return tasks.withType(PackageAndroidArtifact::class.java)
        .firstOrNull { it.variantName == buildVariant.name }
        ?.outputDirectory
        ?.map { directory -> directory.file(fileName) }
        ?: throw GradleException("no output for variant ${buildVariant.name}")
}
