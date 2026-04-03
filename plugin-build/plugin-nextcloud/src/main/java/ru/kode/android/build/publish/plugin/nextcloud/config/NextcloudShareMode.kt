package ru.kode.android.build.publish.plugin.nextcloud.config

/**
 * Share mode used by Nextcloud distribution tasks after upload.
 */
@Suppress("EnumNaming")
enum class NextcloudShareMode {
    INTERNAL_RECIPIENTS,
    PUBLIC_LINK,
}
