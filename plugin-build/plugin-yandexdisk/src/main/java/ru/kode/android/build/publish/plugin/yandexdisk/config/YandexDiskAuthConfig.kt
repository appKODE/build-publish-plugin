package ru.kode.android.build.publish.plugin.yandexdisk.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import ru.kode.android.gradle.commons.util.CommonConfigMergeable

/**
 * Yandex Disk authentication for a build variant (or the common configuration).
 */
interface YandexDiskAuthConfig : CommonConfigMergeable<YandexDiskAuthConfig> {
    val name: String

    /**
     * File containing a Yandex OAuth token with the `cloud_api:disk.write` scope
     * (and `cloud_api:disk.read` to read public links).
     *
     * @see <a href="https://yandex.ru/dev/disk-api/doc/en/concepts/quickstart">Yandex Disk API quickstart</a>
     */
    @get:InputFile
    val oauthTokenFile: RegularFileProperty

    override fun inheritFrom(common: YandexDiskAuthConfig) {
        oauthTokenFile.convention(common.oauthTokenFile)
    }
}
