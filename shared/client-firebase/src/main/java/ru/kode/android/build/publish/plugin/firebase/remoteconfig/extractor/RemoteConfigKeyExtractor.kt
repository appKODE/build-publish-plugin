package ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor

import java.io.Serializable

/**
 * Finds Remote Config keys declared in one source file.
 *
 * Implementations only see a repository-relative [path] and the file content, so the same extractor
 * works for the current release and for an older tag read straight from git. Implementations must be
 * [Serializable] because they are task inputs (configuration cache).
 *
 * Projects can implement their own extractor (for example in `buildSrc`) and pass it to
 * `keySource { extractor.set(...) }`.
 */
fun interface RemoteConfigKeyExtractor : Serializable {
    fun extract(
        path: String,
        content: String,
    ): List<KeyOccurrence>
}
