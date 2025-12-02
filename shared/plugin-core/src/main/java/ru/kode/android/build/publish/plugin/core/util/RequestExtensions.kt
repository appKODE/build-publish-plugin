package ru.kode.android.build.publish.plugin.core.util

import retrofit2.Call
import retrofit2.Response

inline fun <reified T : Any> Call<T>.executeWithResult() = execute().toResult().map { it as T }

fun Call<Unit>.executeNoResult() = execute().toResult().map { }

inline fun <reified T : Any?> Response<T>.toResult(): Result<T?> {
    return if (isSuccessful) {
        val value = body()
        Result.success(value)
    } else {
        mapError<T>()
    }
}

inline fun <reified T : Any?> Response<T>.mapError(): Result<T?> {
    val reason = errorBody()?.string()
    return if (reason?.contains("stream timeout") == true) {
        Result
            .failure(
                RequestError.UploadTimeout(
                    code = code(),
                    reason = reason
                )
            )
    } else {
        Result
            .failure(
                RequestError.Unknown(
                    code = code(),
                    reason = reason
                )
            )
    }
}

sealed class RequestError : Throwable() {
    abstract val code: Int
    abstract val reason: String?

    data class Unknown(
        override val code: Int,
        override val reason: String?
    ) : RequestError()


    data class UploadTimeout(
        override val code: Int,
        override val reason: String?
    ) : RequestError()

}

data class UploadError(
    override val message: String
) : Throwable(message)
