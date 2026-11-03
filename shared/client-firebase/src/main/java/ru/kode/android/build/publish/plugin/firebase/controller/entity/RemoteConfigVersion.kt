package ru.kode.android.build.publish.plugin.firebase.controller.entity

import kotlinx.serialization.Serializable

/**
 * Metadata of a published Remote Config template version.
 *
 * @property versionNumber Monotonically increasing version number (int64 encoded as string by the API).
 * @property updateTime RFC 3339 timestamp of the publication.
 */
@Serializable
data class RemoteConfigVersion(
    val versionNumber: String,
    val updateTime: String? = null,
    val updateUser: User? = null,
    val description: String? = null,
    val updateOrigin: String? = null,
    val updateType: String? = null,
) {
    /**
     * Author of a template version.
     */
    @Serializable
    data class User(
        val email: String? = null,
        val name: String? = null,
    )
}
