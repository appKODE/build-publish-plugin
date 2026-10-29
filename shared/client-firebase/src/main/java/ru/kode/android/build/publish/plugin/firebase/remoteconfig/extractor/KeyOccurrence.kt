package ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor

import kotlinx.serialization.Serializable

/**
 * A Remote Config key found in [path] at 1-based [line].
 */
@Serializable
data class KeyOccurrence(
    val key: String,
    val path: String,
    val line: Int,
) : java.io.Serializable {
    override fun toString(): String = "$path:$line"
}
