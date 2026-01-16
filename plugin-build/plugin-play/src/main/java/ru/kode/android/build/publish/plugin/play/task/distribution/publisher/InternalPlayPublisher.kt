package ru.kode.android.build.publish.plugin.play.task.distribution.publisher

import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.Bundle
import com.google.api.services.androidpublisher.model.DeobfuscationFilesUploadResponse
import com.google.api.services.androidpublisher.model.Track
import java.io.File
import java.io.IOException

/**
 * Internal Play publisher API used by the plugin.
 *
 * This extends [PlayPublisher] with operations that are only needed by the distribution pipeline
 * (e.g., working with tracks and uploading artifacts within an edit).
 */
internal interface InternalPlayPublisher : PlayPublisher {
    /**
     * The Play Console application id (package name).
     */
    val appId: String

    /**
     * Returns the requested track state within the given edit.
     */
    fun getTrack(
        editId: String,
        track: String,
    ): Track

    /**
     * Lists all tracks available within the given edit.
     */
    fun listTracks(editId: String): List<Track>

    /**
     * Updates a track within the given edit.
     */
    fun updateTrack(
        editId: String,
        track: Track,
    )

    /**
     * Uploads an Android App Bundle into the given edit.
     */
    @Throws(IOException::class)
    fun uploadBundle(
        editId: String,
        bundleFile: File,
    ): Bundle

    /**
     * Uploads an APK into the given edit.
     */
    @Throws(IOException::class)
    fun uploadApk(
        editId: String,
        apkFile: File,
    ): Apk

    /**
     * Attaches an expansion file to an existing app version.
     */
    fun attachObb(
        editId: String,
        type: String,
        appVersion: Int,
        obbVersion: Int,
    )

    /**
     * Uploads a deobfuscation file (e.g., Proguard mapping) for a given version.
     */
    fun uploadDeobfuscationFile(
        editId: String,
        file: File,
        versionCode: Int,
        type: String,
    ): DeobfuscationFilesUploadResponse
}
