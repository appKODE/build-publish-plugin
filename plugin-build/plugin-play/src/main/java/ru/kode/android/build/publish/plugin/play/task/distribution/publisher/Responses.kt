package ru.kode.android.build.publish.plugin.play.task.distribution.publisher

import com.google.api.client.googleapis.json.GoogleJsonResponseException

/** Response for an edit request. */
sealed class EditResponse {
    /** Response for a successful edit request. */
    data class Success internal constructor(
        /** The id of the edit in question. */
        val id: String,
    ) : EditResponse()

    /** Response for an unsuccessful edit request. */
    data class Failure internal constructor(
        private val e: GoogleJsonResponseException,
    ) : EditResponse() {
        /** @return true if the app wasn't found in the Play Console, false otherwise */
        fun isNewApp(): Boolean = e has "applicationNotFound"
    }
}

/** Response for an commit request. */
sealed class CommitResponse {
    /** Response for a successful commit request. */
    object Success : CommitResponse()

    /** Response for an unsuccessful commit request. */
    data class Failure internal constructor(
        private val e: GoogleJsonResponseException,
    ) : CommitResponse()
}

/** Response for an internal sharing artifact upload. */
data class UploadInternalSharingArtifactResponse internal constructor(
    /** The response's full JSON payload. */
    val json: String,
    /** The download URL of the uploaded artifact. */
    val downloadUrl: String,
)

/** Response for a product request. */
data class GppProduct internal constructor(
    /** The product ID. */
    val sku: String,
    /** The response's full JSON payload. */
    val json: String,
)

/** Response for a product update request. */
data class UpdateProductResponse internal constructor(
    /** @return true if the product doesn't exist and needs to be created, false otherwise. */
    val needsCreating: Boolean,
)
