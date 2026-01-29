package ru.kode.android.build.publish.plugin.core.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.Call
import retrofit2.Response

val errorJson = Json { ignoreUnknownKeys = true }

inline fun <reified T : Any> Call<T>.executeWithResult(): Result<T> {
    return execute().toResult().map { it as T }
}

fun Call<Unit>.executeNoResult(): Result<Unit> {
    return execute().toResult().map { }
}

inline fun <reified T : Any?> Response<T>.toResult(): Result<T?> {
    return if (isSuccessful) {
        val value = body()
        Result.success(value)
    } else {
        mapError<T>()
    }
}

inline fun <reified T : Any?> Response<T>.mapError(): Result<T?> {
    val errorBody = errorBody()?.string()
    val message =
        errorBody
            ?.takeIf { it.isNotBlank() }
            ?.let { errorJson.decodeFromString<ApiErrorWithDescription>(it).description }
            ?: message()
    return if (errorBody?.contains("stream timeout") == true) {
        Result
            .failure(
                RequestError.UploadTimeout(
                    code = code(),
                    reason = errorBody,
                    message = message,
                ),
            )
    } else {
        Result
            .failure(
                RequestError.Unknown(
                    code = code(),
                    reason = errorBody,
                    message = message,
                ),
            )
    }
}

sealed class RequestError : Throwable() {
    abstract val code: Int
    abstract val reason: String?

    data class Unknown(
        override val code: Int,
        override val reason: String?,
        override val message: String?,
    ) : RequestError()

    data class UploadTimeout(
        override val code: Int,
        override val reason: String?,
        override val message: String?,
    ) : RequestError()
}

data class UploadError(
    override val message: String,
) : Throwable(message)

@Serializable
data class ApiErrorWithDescription(
    val description: String? = null,
)
