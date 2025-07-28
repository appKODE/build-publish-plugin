package ru.kode.android.build.publish.plugin.util

import okhttp3.Credentials
import okhttp3.OkHttpClient
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
        .proxySelector(DynamicProxySelector())
        .proxyAuthenticator { _, response ->
            val url = response.request.url
            val proxyUser =
                if (url.scheme == "https") {
                    System.getProperty("https.proxyUser")
                } else {
                    System.getProperty("http.proxyUser")
                }
            val proxyPassword =
                if (url.scheme == "https") {
                    System.getProperty("https.proxyPassword")
                } else {
                    System.getProperty("http.proxyPassword")
                }
            val credentials = Credentials.basic(proxyUser!!, proxyPassword!!)
            response.request.newBuilder()
                .header("Proxy-Authorization", credentials)
                .build()
        }
}

private class DynamicProxySelector : ProxySelector() {
    override fun select(uri: URI?): List<Proxy> {
        val proxyHost =
            if (uri?.scheme == "https") {
                System.getProperty("https.proxyHost")
            } else {
                System.getProperty("http.proxyHost")
            }
        val proxyPort =
            if (uri?.scheme == "https") {
                System.getProperty("https.proxyPort")
            } else {
                System.getProperty("http.proxyPort")
            }
        val nonProxyHosts =
            if (uri?.scheme == "https") {
                System.getProperty("https.nonProxyHosts")
            } else {
                System.getProperty("http.nonProxyHosts")
            }

        if (proxyHost != null && proxyPort != null) {
            val host = uri?.host ?: return listOf(Proxy.NO_PROXY)
            if (nonProxyHosts != null && nonProxyHosts.split("|").any { host.contains(it) }) {
                return listOf(Proxy.NO_PROXY)
            }
            val proxyAddress = InetSocketAddress.createUnresolved(proxyHost, proxyPort.toInt())
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
