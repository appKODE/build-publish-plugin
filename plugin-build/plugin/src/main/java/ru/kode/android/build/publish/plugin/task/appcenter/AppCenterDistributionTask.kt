package ru.kode.android.build.publish.plugin.task.appcenter

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import ru.kode.android.build.publish.plugin.task.appcenter.entity.ChunkRequestBody
import ru.kode.android.build.publish.plugin.task.appcenter.uploader.AppCenterUploader
import java.io.IOException

abstract class AppCenterDistributionTask : DefaultTask() {
    init {
        description = "Task to send apk to AppCenter"
        group = BasePlugin.BUILD_GROUP
    }

    @get:Input
    @get:Option(
        option = "buildVariantOutputFile",
        description = "Artifact output file (absolute path is expected)"
    )
    abstract val buildVariantOutputFile: RegularFileProperty

    @get:Input
    @get:Option(
        option = "ownerName",
        description = "Owner name of target project in AppCenter"
    )
    abstract val ownerName: Property<String>

    @get:Input
    @get:Option(
        option = "appName",
        description = "Application prefix for application name in AppCenter"
    )
    abstract val appName: Property<String>

    @get:Input
    @get:Option(
        option = "apiToken",
        description = "API token for target project in AppCenter"
    )
    abstract val apiToken: Property<String>

    @get:Input
    @get:Option(option = "testerGroups", description = "Distribution group names")
    abstract val testerGroups: SetProperty<String>

    @get:InputFile
    @get:Option(
        option = "changelogFile",
        description = "File with saved changelog"
    )
    abstract val changelogFile: RegularFileProperty

    @TaskAction
    fun upload() {
        val uploader = AppCenterUploader(
            ownerName.get(),
            appName.get(),
            project.logger,
            apiToken.get()
        )
        project.logger.debug("Step 1/7: Prepare upload")
        val prepareResponse = uploader.prepareRelease()
        uploader.iniUploadApi(prepareResponse.upload_domain ?: "https://file.appcenter.ms")

        project.logger.debug("Step 2/7: Send metadata")
        val apkFile = buildVariantOutputFile.get().asFile
        require(apkFile.isFile && apkFile.extension == "apk") {
            "File ${apkFile.path} is not apk"
        }
        val packageAssetId = prepareResponse.package_asset_id
        val encodedToken = prepareResponse.url_encoded_token
        val metaResponse = uploader.sendMetaData(apkFile, packageAssetId, encodedToken)

        // See NOTE_CHUNKS_UPLOAD_LOOP
        project.logger.debug("Step 3/7: Upload apk file chunks")
        metaResponse.chunk_list.forEachIndexed { i, chunkNumber ->
            val range = (i * metaResponse.chunk_size)..((i + 1) * metaResponse.chunk_size)
            project.logger.debug("Step 3/7 : Upload chunk ${i + 1}/${metaResponse.chunk_list.size}")
            uploader.uploadChunk(
                packageAssetId = packageAssetId,
                encodedToken = encodedToken,
                chunkNumber = chunkNumber,
                request = ChunkRequestBody(apkFile, range, "application/octet-stream")
            )
        }

        project.logger.debug("Step 4/7: Finish upload")
        uploader.sendUploadIsFinished(packageAssetId, encodedToken)

        project.logger.debug("Step 5/7: Commit uploaded release")
        uploader.commit(prepareResponse.id)

        project.logger.debug("Step 6/7: Fetching for release to be ready to publish")
        val publishResponse = uploader.waitingReadyToBePublished(prepareResponse.id)
        project.logger.debug("Step 7/7: Distribute to the app testers: $testerGroups")
        val releaseId = publishResponse.release_distinct_id
        val releaseNotes = try {
            changelogFile.get().asFile.readText()
        } catch (e: IOException) {
            project.logger.error("failed to read changelog $e"); null
        }
        if (releaseId != null) {
            uploader.distribute(releaseId, testerGroups.get(), releaseNotes.orEmpty())
        } else {
            project.logger.error(
                "Apk was uploaded, " +
                    "but distributors will not be notified: " +
                    "field 'release_distinct_id' is null, cannot execute 'distribute' request"
            )
        }
        project.logger.debug("Done")
    }
}
