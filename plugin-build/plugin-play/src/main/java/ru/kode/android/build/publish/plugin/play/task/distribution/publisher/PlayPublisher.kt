package ru.kode.android.build.publish.plugin.play.task.distribution.publisher

import java.io.File

/**
 * Proxy for the AndroidPublisher API. Separate the build side configuration from API dependencies
 * to make testing easier.
 *
 * For the full API docs, see [here](https://developers.google.com/android-publisher/api-ref).
 */
internal interface PlayPublisher {
    /**
     * Creates a new edit.
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/edits/insert).
     */
    fun insertEdit(): EditResponse

    /**
     * Retrieves an existing edit with the given [id].
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/edits/get).
     */
    fun getEdit(id: String): EditResponse

    /**
     * Commits an edit with the given [id].
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/edits/commit).
     */
    fun commitEdit(
        id: String,
        sendChangesForReview: Boolean = true,
    ): CommitResponse

    /**
     * Validates an edit with the given [id].
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/edits/validate).
     */
    fun validateEdit(id: String)

    /**
     * Uploads the given [bundleFile] as an Internal Sharing artifact.
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/internalappsharingartifacts/uploadbundle).
     */
    fun uploadInternalSharingBundle(bundleFile: File): UploadInternalSharingArtifactResponse

    /**
     * Uploads the given [apkFile] as an Internal Sharing artifact.
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/internalappsharingartifacts/uploadapk).
     */
    fun uploadInternalSharingApk(apkFile: File): UploadInternalSharingArtifactResponse

    /**
     * Get all current products.
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/inappproducts/list).
     */
    fun getInAppProducts(): List<GppProduct>

    /**
     * Creates a new product from the given [productFile].
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/inappproducts/insert).
     */
    fun insertInAppProduct(productFile: File)

    /**
     * Updates an existing product from the given [productFile].
     *
     * More docs are available
     * [here](https://developers.google.com/android-publisher/api-ref/inappproducts/update).
     */
    fun updateInAppProduct(productFile: File): UpdateProductResponse
}
