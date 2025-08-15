package ru.kode.android.build.publish.plugin.appcenter.task.distribution

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.appcenter.config.MAX_REQUEST_COUNT
import ru.kode.android.build.publish.plugin.appcenter.config.MAX_REQUEST_DELAY_MS
import ru.kode.android.build.publish.plugin.appcenter.service.network.AppCenterNetworkService
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.work.AppCenterUploadWork
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.util.capitalized
import javax.inject.Inject

/**
 * Gradle task for uploading an APK build artifact to Microsoft AppCenter.
 *
 * This task takes a built APK along with release metadata (changelog, build info, tester groups)
 * and sends it to the specified AppCenter application. Upload parameters can be configured via
 * Gradle properties or CLI `--option` arguments.
 *
 * ## Behavior
 * 1. Validates that the provided output file is an APK.
 * 2. Reads build metadata from [buildTagFile].
 * 3. Creates a non-isolated [WorkQueue] for background upload.
 * 4. Submits an [AppCenterUploadWork] unit with all configured parameters.
 *
 * @throws GradleException if [distributionFile] is not an APK.
 */
abstract class AppCenterDistributionTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Uploads an APK build artifact to Microsoft AppCenter."
            group = BasePlugin.BUILD_GROUP
        }

        /**
         *  Instance of [AppCenterNetworkService] used to communicate with AppCenter API.
         */
        @get:Internal
        abstract val networkService: Property<AppCenterNetworkService>

        @get:InputFile
        @get:Option(
            option = "changelogFile",
            description = "Path to a file containing the release changelog.",
        )
        abstract val changelogFile: RegularFileProperty

        @get:InputFile
        @get:Option(
            option = "distributionFile",
            description = "Absolute path to the APK file to upload.",
        )
        abstract val distributionFile: RegularFileProperty

        @get:InputFile
        @get:Option(
            option = "buildTagFile",
            description = "Path to a JSON file containing build metadata (name, number, variant).",
        )
        abstract val buildTagFile: RegularFileProperty

        @get:Input
        @get:Optional
        @get:Option(
            option = "appName",
            description =
                "Application name in AppCenter. Defaults to '<baseFileName>-<variant>' " +
                    "if not set.",
        )
        abstract val appName: Property<String>

        @get:Input
        @get:Option(
            option = "testerGroups",
            description = "Comma-separated list of AppCenter distribution group names.",
        )
        abstract val testerGroups: SetProperty<String>

        @get:Input
        @get:Optional
        @get:Option(
            option = "maxUploadStatusRequestCount",
            description =
                "Maximum number of polling requests for upload status. " +
                    "Default = $MAX_REQUEST_COUNT.",
        )
        abstract val maxUploadStatusRequestCount: Property<Int>

        @get:Input
        @get:Optional
        @get:Option(
            option = "uploadStatusRequestDelayMs",
            description =
                "Delay in milliseconds between upload status polling requests. " +
                    "Default = $MAX_REQUEST_DELAY_MS.",
        )
        abstract val uploadStatusRequestDelayMs: Property<Long>

        @get:Input
        @get:Optional
        @get:Option(
            option = "uploadStatusRequestDelayCoefficient",
            description =
                "If greater than 0, polling delay (in seconds) is calculated as " +
                    "'APK size (MB) / coefficient'. Otherwise, 'uploadStatusRequestDelayMs' is used.",
        )
        abstract val uploadStatusRequestDelayCoefficient: Property<Long>

        @get:Input
        @get:Option(
            option = "baseFileName",
            description = "Base file name prefix for the APK artifact.",
        )
        abstract val baseFileName: Property<String>

        @TaskAction
        fun upload() {
            val outputFile = distributionFile.asFile.get()
            if (outputFile.extension != "apk") throw GradleException("file ${outputFile.path} is not apk")
            val currentBuildTag = fromJson(buildTagFile.asFile.get())
            val changelogFile = changelogFile.asFile.get()
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(AppCenterUploadWork::class.java) { parameters ->
                parameters.appName.set(
                    if (appName.isPresent) {
                        appName
                    } else {
                        baseFileName.map { baseFileNameValue ->
                            "${baseFileNameValue.capitalized()}-${currentBuildTag.buildVariant}"
                        }
                    },
                )
                parameters.buildName.set(currentBuildTag.name)
                parameters.buildNumber.set(currentBuildTag.buildNumber.toString())
                parameters.outputFile.set(outputFile)
                parameters.testerGroups.set(testerGroups)
                parameters.changelogFile.set(changelogFile)
                parameters.maxUploadStatusRequestCount.set(maxUploadStatusRequestCount)
                parameters.uploadStatusRequestDelayMs.set(uploadStatusRequestDelayMs)
                parameters.uploadStatusRequestDelayCoefficient.set(uploadStatusRequestDelayCoefficient)
            }
        }
    }
