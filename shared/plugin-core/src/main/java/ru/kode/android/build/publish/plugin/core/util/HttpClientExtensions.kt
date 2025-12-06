package ru.kode.android.build.publish.plugin.core.util

import okhttp3.Credentials
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.gradle.api.logging.Logging
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

private val logger = Logging.getLogger("HttpClientExtensions")

@Suppress("ComplexCondition") // Just checking for all proxy properties
fun OkHttpClient.Builder.addProxyIfAvailable(): OkHttpClient.Builder {
    return this
        .addNetworkInterceptor { chain ->
            val request = chain.request()
            val proxy = chain.connection()?.route()?.proxy ?: Proxy.NO_PROXY
            logger.info("Requesting via proxy $proxy: ${request.url}")
            chain.proceed(request)
        }
        .proxySelector(DynamicProxySelector())
        .proxyAuthenticator { _, response ->
            val url = response.request.url

            val proxyUser =
                if (url.scheme == "https") {
                    getEnvOrProperty("https.proxyUser")
                } else {
                    getEnvOrProperty("http.proxyUser")
                }
            val proxyPassword =
                if (url.scheme == "https") {
                    getEnvOrProperty("https.proxyPassword")
                } else {
                    getEnvOrProperty("http.proxyPassword")
                }
            val credentials = Credentials.basic(proxyUser!!, proxyPassword!!)
            logger.info("Apply proxy authorization")
            response.request.newBuilder()
                .header("Proxy-Authorization", credentials)
                .build()
        }
}

private class DynamicProxySelector : ProxySelector() {
    override fun select(uri: URI?): List<Proxy> {
        val proxyHost =
            if (uri?.scheme == "https") {
                getEnvOrProperty("https.proxyHost")
            } else {
                getEnvOrProperty("http.proxyHost")
            }
        val proxyPort =
            if (uri?.scheme == "https") {
                getEnvOrProperty("https.proxyPort")
            } else {
                getEnvOrProperty("http.proxyPort")
            }
        val nonProxyHosts =
            if (uri?.scheme == "https") {
                getEnvOrProperty("https.nonProxyHosts")
            } else {
                getEnvOrProperty("http.nonProxyHosts")
            }

        if (proxyHost != null && proxyPort != null) {
            val host = uri?.host ?: return listOf(Proxy.NO_PROXY)
            if (nonProxyHosts != null && nonProxyHosts.split("|").any { host.contains(it) }) {
                return listOf(Proxy.NO_PROXY)
            }
            val proxyAddress = InetSocketAddress.createUnresolved(proxyHost, proxyPort.toInt())
            logger.info("Return and apply proxy: url=$uri; proxy address=$proxyAddress")
            return listOf(Proxy(Proxy.Type.HTTP, proxyAddress))
        }
        return listOf(Proxy.NO_PROXY)
    }

    override fun connectFailed(
        uri: URI?,
        address: SocketAddress?,
        error: IOException?,
    ) {
        logger.error("Proxy connection failed for $uri", error)
    }
}

private fun getEnvOrProperty(key: String): String? {
    return System.getenv(key) ?: System.getProperty(key)
}

fun createPartFromString(value: String): RequestBody {
    return value.toRequestBody(MultipartBody.FORM)
}
