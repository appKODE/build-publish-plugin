package ru.kode.android.build.publish.plugin.play.task.distribution.publisher

import com.google.api.client.googleapis.json.GoogleJsonResponseException

/** Response for an edit request. */
internal sealed class EditResponse {
    /** Response for a successful edit request. */
    internal data class Success(
        /** The id of the edit in question. */
        val id: String,
    ) : EditResponse()

    /** Response for an unsuccessful edit request. */
    internal data class Failure(
        private val e: GoogleJsonResponseException,
    ) : EditResponse() {
        /** @return true if the app wasn't found in the Play Console, false otherwise */
        fun isNewApp(): Boolean = e has "applicationNotFound"
    }
}

/** Response for an commit request. */
internal sealed class CommitResponse {
    /** Response for a successful commit request. */
    internal object Success : CommitResponse()

    /** Response for an unsuccessful commit request. */
    internal data class Failure(
        private val e: GoogleJsonResponseException,
    ) : CommitResponse()
}

/** Response for an internal sharing artifact upload. */
internal data class UploadInternalSharingArtifactResponse(
    /** The response's full JSON payload. */
    val json: String,
    /** The download URL of the uploaded artifact. */
    val downloadUrl: String,
)

/** Response for a product request. */
internal data class GppProduct(
    /** The product ID. */
    val sku: String,
    /** The response's full JSON payload. */
    val json: String,
)

/** Response for a product update request. */
internal data class UpdateProductResponse(
    /** @return true if the product doesn't exist and needs to be created, false otherwise. */
    val needsCreating: Boolean,
)
