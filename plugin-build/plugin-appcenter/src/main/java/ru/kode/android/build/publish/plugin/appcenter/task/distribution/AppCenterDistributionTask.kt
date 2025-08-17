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
 * Gradle task that handles the upload of Android application packages (APKs) to AppCenter.
 *
 * This task is responsible for:
 * - Validating the input APK file
 * - Reading build metadata and changelog
 * - Coordinating the upload process using [AppCenterUploadWork]
 * - Handling distribution to specified tester groups
 *
 * @see AppCenterUploadWork For the actual upload implementation
 * @see AppCenterNetworkService For API communication with AppCenter
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
         * The AppCenter network service used for API communication.
         *
         * This is injected by Gradle and provides the underlying HTTP client
         * and API methods for interacting with AppCenter's distribution APIs.
         */
        @get:Internal
        abstract val networkService: Property<AppCenterNetworkService>

        /**
         * File containing the release notes or changelog for this distribution.
         *
         * The contents of this file will be displayed to testers in the AppCenter portal
         * and included in distribution emails. Supports Markdown formatting.
         */
        @get:InputFile
        @get:Option(
            option = "changelogFile",
            description = "Path to a file containing the release changelog.",
        )
        abstract val changelogFile: RegularFileProperty

        /**
         * The Android application package (APK) file to be uploaded to AppCenter.
         *
         * This must be a valid APK file. The task will fail if the file doesn't exist
         * or has an invalid format.
         */
        @get:InputFile
        @get:Option(
            option = "distributionFile",
            description = "Absolute path to the APK file to upload.",
        )
        abstract val distributionFile: RegularFileProperty

        /**
         * JSON file containing metadata about the build being distributed.
         */
        @get:InputFile
        @get:Option(
            option = "buildTagFile",
            description = "Path to a JSON file containing build metadata (name, number, variant).",
        )
        abstract val buildTagFile: RegularFileProperty

        /**
         * The name of the application in AppCenter.
         *
         * This should match the application name as registered in your AppCenter account.
         * If not specified, it will be generated as '<baseFileName>-<variant>'.
         *
         * Example: "MyApp-Android"
         */
        @get:Input
        @get:Optional
        @get:Option(
            option = "appName",
            description =
                "Application name in AppCenter. Defaults to '<baseFileName>-<variant>' " +
                    "if not set.",
        )
        abstract val appName: Property<String>

        /**
         * Set of distribution group names that should receive this build.
         *
         * These groups must already exist in your AppCenter organization.
         * Testers in these groups will be notified about the new release.
         *
         * Example: `setOf("beta-testers", "internal-team")`
         */
        @get:Input
        @get:Option(
            option = "testerGroups",
            description = "Comma-separated list of AppCenter distribution group names.",
        )
        abstract val testerGroups: SetProperty<String>

        /**
         * Maximum number of times to poll for upload status before timing out.
         *
         * The task will poll AppCenter's API to check the status of the upload.
         * This controls how many times it will check before giving up.
         *
         * Default: [MAX_REQUEST_COUNT]
         */
        @get:Input
        @get:Optional
        @get:Option(
            option = "maxUploadStatusRequestCount",
            description =
                "Maximum number of polling requests for upload status. " +
                    "Default = $MAX_REQUEST_COUNT.",
        )
        abstract val maxUploadStatusRequestCount: Property<Int>

        /**
         * Delay in milliseconds between upload status polling requests.
         *
         * The task will poll AppCenter's API to check the status of the upload.
         * This controls the time between each polling request.
         *
         * Default: [MAX_REQUEST_DELAY_MS]
         */
        @get:Input
        @get:Optional
        @get:Option(
            option = "uploadStatusRequestDelayMs",
            description =
                "Delay in milliseconds between upload status polling requests. " +
                    "Default = $MAX_REQUEST_DELAY_MS.",
        )
        abstract val uploadStatusRequestDelayMs: Property<Long>

        /**
         * If greater than 0, polling delay (in milliseconds) is calculated as
         * 'APK size (MB) / coefficient' * 1000. Otherwise, 'uploadStatusRequestDelayMs' is used.
         *
         * Example: If coefficient is 10 and APK size is 5 MB, the delay will be 50 seconds.
         *
         * Default: 0 (no dynamic delay)
         */
        @get:Input
        @get:Optional
        @get:Option(
            option = "uploadStatusRequestDelayCoefficient",
            description =
                "If greater than 0, polling delay (in seconds) is calculated as " +
                    "'APK size (MB) / coefficient'. Otherwise, 'uploadStatusRequestDelayMs' is used.",
        )
        abstract val uploadStatusRequestDelayCoefficient: Property<Long>

        /**
         * Base file name prefix for the APK artifact.
         *
         * This value is used to generate the final file name for the APK artifact.
         * It is concatenated with the build variant name to form the full file name.
         *
         * Example: If `baseFileName` is set to "MyApp", and the build variant is "debug",
         * the final file name will be "MyApp-debug.apk".
         */
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
