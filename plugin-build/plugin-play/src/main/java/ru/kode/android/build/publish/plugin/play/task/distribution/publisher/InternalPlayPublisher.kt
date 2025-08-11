package ru.kode.android.build.publish.plugin.play.task.distribution.publisher

import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.Bundle
import com.google.api.services.androidpublisher.model.DeobfuscationFilesUploadResponse
import com.google.api.services.androidpublisher.model.Track
import java.io.File
import java.io.IOException

internal interface InternalPlayPublisher : PlayPublisher {
    val appId: String

    fun getTrack(
        editId: String,
        track: String,
    ): Track

    fun listTracks(editId: String): List<Track>

    fun updateTrack(
        editId: String,
        track: Track,
    )

    @Throws(IOException::class)
    fun uploadBundle(
        editId: String,
        bundleFile: File,
    ): Bundle

    @Throws(IOException::class)
    fun uploadApk(
        editId: String,
        apkFile: File,
    ): Apk

    fun attachObb(
        editId: String,
        type: String,
        appVersion: Int,
        obbVersion: Int,
    )

    fun uploadDeobfuscationFile(
        editId: String,
        file: File,
        versionCode: Int,
        type: String,
    ): DeobfuscationFilesUploadResponse
}
