package ru.kode.android.build.publish.plugin.yandexdisk.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import ru.kode.android.gradle.commons.util.CommonConfigMergeable

/**
 * Where and how build artifacts are uploaded to Yandex Disk.
 */
interface YandexDiskDistributionConfig : CommonConfigMergeable<YandexDiskDistributionConfig> {
    val name: String

    /**
     * Folder on the disk, relative to its root, for example `builds/android`.
     * Missing folders are created.
     */
    @get:Input
    val remotePath: Property<String>

    /**
     * Whether to zip the artifact before uploading. Defaults to `false`.
     */
    @get:Input
    @get:Optional
    val compressed: Property<Boolean>

    /**
     * Whether to publish the uploaded file and log its public link. Defaults to `false`:
     * the file stays private to the disk owner.
     */
    @get:Input
    @get:Optional
    val publicLink: Property<Boolean>

    /**
     * Explicit remote file name. Defaults to `<baseFileName>-<version>-<variant>.<ext>`.
     */
    @get:Input
    @get:Optional
    val remoteFileName: Property<String>

    override fun inheritFrom(common: YandexDiskDistributionConfig) {
        remotePath.convention(common.remotePath)
        compressed.convention(common.compressed)
        publicLink.convention(common.publicLink)
        remoteFileName.convention(common.remoteFileName)
    }
}
