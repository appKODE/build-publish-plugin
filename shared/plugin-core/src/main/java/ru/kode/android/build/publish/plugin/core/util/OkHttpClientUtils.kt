package ru.kode.android.build.publish.plugin.core.util

import okhttp3.logging.HttpLoggingInterceptor
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger

fun buildLoggingInterceptor(logger: PluginLogger): HttpLoggingInterceptor =
    HttpLoggingInterceptor { message ->
        if (!message.contains("Content-Disposition: form-data")) {
            logger.info(SecretRedaction.redactUrl(message))
        }
    }.apply {
        // Never emit credential-bearing headers, regardless of the logging level below.
        redactHeader("Authorization")
        redactHeader("Proxy-Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
        level =
            if (logger.bodyLogging) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.HEADERS
            }
    }
