package ru.kode.android.build.publish.plugin.play.task.distribution.publisher

import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpResponseException
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.Bundle
import com.google.api.services.androidpublisher.model.DeobfuscationFilesUploadResponse
import com.google.api.services.androidpublisher.model.Track
import java.io.File

/**
 * In-memory [InternalPlayPublisher] used to drive track/edit managers deterministically in tests.
 *
 * Only the operations exercised by [ru.kode.android.build.publish.plugin.play.task.distribution.track]
 * managers are backed by real state; the rest fail loudly if a test accidentally reaches them.
 */
internal class FakeInternalPlayPublisher(
    override val appId: String = "com.example.app",
) : InternalPlayPublisher {
    /** Canned tracks returned by [getTrack], keyed by track name. */
    val tracks: MutableMap<String, Track> = mutableMapOf()

    /** Tracks captured from every [updateTrack] call, in order. */
    val updatedTracks: MutableList<Track> = mutableListOf()

    /** When set, [uploadBundle] throws it instead of returning [uploadBundleResult]. */
    var uploadBundleError: GoogleJsonResponseException? = null

    /** Bundle returned by a successful [uploadBundle]. */
    var uploadBundleResult: Bundle = Bundle().apply { versionCode = DEFAULT_VERSION_CODE }

    override fun getTrack(
        editId: String,
        track: String,
    ): Track = tracks[track] ?: Track().setTrack(track)

    override fun listTracks(editId: String): List<Track> = tracks.values.toList()

    override fun updateTrack(
        editId: String,
        track: Track,
    ) {
        updatedTracks += track
    }

    override fun uploadBundle(
        editId: String,
        bundleFile: File,
    ): Bundle = uploadBundleError?.let { throw it } ?: uploadBundleResult

    override fun uploadApk(
        editId: String,
        apkFile: File,
    ): Apk = unsupported()

    override fun attachObb(
        editId: String,
        type: String,
        appVersion: Int,
        obbVersion: Int,
    ) = unsupported()

    override fun uploadDeobfuscationFile(
        editId: String,
        file: File,
        versionCode: Int,
        type: String,
    ): DeobfuscationFilesUploadResponse = unsupported()

    override fun insertEdit(): EditResponse = unsupported()

    override fun getEdit(id: String): EditResponse = unsupported()

    override fun commitEdit(
        id: String,
        sendChangesForReview: Boolean,
    ): CommitResponse = unsupported()

    override fun validateEdit(id: String) = unsupported()

    override fun uploadInternalSharingBundle(bundleFile: File): UploadInternalSharingArtifactResponse = unsupported()

    override fun uploadInternalSharingApk(apkFile: File): UploadInternalSharingArtifactResponse = unsupported()

    override fun getInAppProducts(): List<GppProduct> = unsupported()

    override fun insertInAppProduct(productFile: File) = unsupported()

    override fun updateInAppProduct(productFile: File): UpdateProductResponse = unsupported()

    private fun unsupported(): Nothing = error("Not needed for track/edit manager tests")

    private companion object {
        const val DEFAULT_VERSION_CODE = 100
    }
}

/**
 * Builds a [GoogleJsonResponseException] carrying the given error [reason] (and optional
 * `message`), matching what [has] and the upload-failure handling inspect.
 */
internal fun googleJsonException(
    reason: String,
    message: String? = null,
    statusCode: Int = 403,
): GoogleJsonResponseException {
    val details =
        GoogleJsonError().apply {
            this.message = message
            errors = listOf(GoogleJsonError.ErrorInfo().apply { this.reason = reason })
        }
    val builder = HttpResponseException.Builder(statusCode, "error", HttpHeaders())
    return GoogleJsonResponseException(builder, details)
}
