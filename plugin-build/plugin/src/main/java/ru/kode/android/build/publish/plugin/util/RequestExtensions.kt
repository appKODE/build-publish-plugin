package ru.kode.android.build.publish.plugin.util

import org.gradle.api.logging.Logging
import retrofit2.Call
import retrofit2.Response

fun <T> Call<T>.executeOrThrow() = execute().bodyOrThrow()!!

fun <T> Call<T>.executeOptionalOrThrow() = execute().bodyOrThrow()

fun <T> Call<T>.executeOptionalOrLogError() = execute().bodyOrLogError()

fun <T> Response<T>.bodyOrThrow() = successOrThrow()

fun <T> Response<T>.bodyOrLogError() = successOrLogError()

fun <T> Response<T>.successOrThrow() =
    if (isSuccessful) {
        body()
    } else {
        val reason = errorBody()?.string()
        if (reason == "stream timeout") {
            throw UploadStreamTimeoutException()
        } else {
            throw UploadException(
                "Upload error, code=${code()}, reason=$reason",
            )
        }
    }

fun <T> Response<T>.successOrLogError(): T? =
    if (isSuccessful) {
        body()
    } else {
        val logger = Logging.getLogger(this::class.java)

        val reason = errorBody()?.string()
        logger.error("Jira automation error, code=${code()}, reason=$reason")
        null
    }

internal class UploadStreamTimeoutException : Throwable()
internal class UploadException(override val message: String) : Throwable(message)
