package ru.kode.android.build.publish.plugin.util

import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.gradle.api.logging.Logging
import java.net.InetSocketAddress
import java.net.Proxy

private val logger = Logging.getLogger("HttpClientExtensions")

@Suppress("ComplexCondition") // Just checking for all proxy properties
fun OkHttpClient.Builder.addProxyIfAvailable(): OkHttpClient.Builder {
    val proxyHost = System.getProperty("http.proxyHost")
    val proxyPort = System.getProperty("http.proxyPort")
    val proxyUser = System.getProperty("http.proxyUser")
    val proxyPassword = System.getProperty("http.proxyPassword")

    return if (proxyHost != null && proxyPort != null && proxyUser != null && proxyPassword != null) {
        logger.info(
            "Applied proxy: " +
                "proxyHost=$proxyHost, " +
                "proxyPort=$proxyPort, " +
                "proxyUser=$proxyUser, " +
                "proxyPassword=${proxyPassword.mask()}",
        )
        val proxySocketAddress = InetSocketAddress.createUnresolved(proxyHost, proxyPort.toInt())
        this.proxy(Proxy(Proxy.Type.HTTP, proxySocketAddress))
            .proxyAuthenticator { _, response ->
                val credentials = Credentials.basic(proxyUser, proxyPassword)
                response.request.newBuilder()
                    .header("Proxy-Authorization", credentials)
                    .build()
            }
    } else {
        this
    }
}
