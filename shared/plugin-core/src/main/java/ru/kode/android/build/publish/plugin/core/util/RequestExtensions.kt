package ru.kode.android.build.publish.plugin.core.util

import retrofit2.Call
import retrofit2.Response

fun <T> Call<T>.executeOrThrow() = execute().bodyOrThrow()!!

fun <T> Call<T>.executeOptionalOrThrow() = execute().bodyOrThrow()

fun <T> Response<T>.bodyOrThrow() = successOrThrow()

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

class UploadStreamTimeoutException : Throwable()

class UploadException(override val message: String) : Throwable(message)
