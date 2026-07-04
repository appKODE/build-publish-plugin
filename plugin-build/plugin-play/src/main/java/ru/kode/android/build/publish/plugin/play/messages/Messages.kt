@file:Suppress("FunctionOnlyReturningConstant") // Simple string providers

package ru.kode.android.build.publish.plugin.play.messages

import ru.kode.android.build.publish.plugin.core.util.capitalized
import java.io.File

fun stepRequestingTrackEditMessage(): String = "Step 1/4: Requesting track edit..."

fun errorAppDoesNotExistMessage(): String = "Error response: app does not exist"

fun errorResponseMessage(response: String): String = "Error response: $response"

fun failedToFetchEditIdMessage(): String = "Failed to fetch edit id to upload bundle"

fun stepUploadBundleMessage(editId: String): String = "Step 2/4: Upload bundle for $editId"

fun failedToUploadBundleMessage(): String = "Failed to upload bundle"

fun stepPushingReleaseMessage(
    releaseName: String,
    trackId: String,
    priority: Int,
    versionCode: Long,
): String = "Step 3/4: Pushing $releaseName to $trackId at P=$priority V=$versionCode"

fun stepCommitEditMessage(editId: String): String = "Step 3/4: Commit $editId"

fun stepBundleUploadSuccessfulMessage(): String = "Step 4/4: Bundle upload successful"

fun promotingReleaseMessage(trackName: String?): String = "Promoting release from track '$trackName'"

fun updatingTrackMessage(
    statuses: List<*>,
    appId: String,
    versionCodes: List<*>,
    trackName: String?,
): String = "Updating $statuses release ($appId:$versionCodes) in track '$trackName'"

fun startingUploadMessage(
    thing: String,
    file: File,
): String = "Starting $thing upload: $file"

fun uploadingProgressMessage(
    thing: String,
    percent: Int,
): String = "Uploading $thing: $percent% complete"

fun uploadCompleteMessage(thing: String): String = "${thing.capitalized()} upload complete"
