package ru.kode.android.build.publish.plugin.core.util

import okhttp3.Credentials
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.messages.applyProxyAuthMessage
import ru.kode.android.build.publish.plugin.core.messages.cannotCreateHttpProxyMessage
import ru.kode.android.build.publish.plugin.core.messages.cannotCreateHttpsProxyMessage
import ru.kode.android.build.publish.plugin.core.messages.createHttpProxyMessage
import ru.kode.android.build.publish.plugin.core.messages.createHttpsProxyMessage
import ru.kode.android.build.publish.plugin.core.messages.proxyConnectionFailedMessage
import ru.kode.android.build.publish.plugin.core.messages.proxyCredsNotSpecified
import ru.kode.android.build.publish.plugin.core.messages.requestingProxyMessage
import ru.kode.android.build.publish.plugin.core.messages.requestingWithoutProxyMessage
import ru.kode.android.build.publish.plugin.core.messages.returnAndApplyProxyMessage
import java.io.IOException
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

/**
 * Custom authenticator for proxy authentication.
 *
 * This class extends [Authenticator] to provide credentials when connecting through
 * a proxy server that requires authentication. It retrieves the username and password
 * from the provided [NetworkProxy] configuration.
 *
 * @property logger the logger used for logging authentication events
 * @property networkProxy the network proxy configuration containing authentication credentials
 */
class ProxyAuthenticator(
    val logger: PluginLogger,
    val networkProxy: NetworkProxy,
) : Authenticator() {
    override fun getPasswordAuthentication(): PasswordAuthentication {
        val userName = networkProxy.user
        val userPassword = networkProxy.password
        logger.info("🎯 AUTHENTICATOR TRIGGERED!")
        logger.info("  Host: $requestingHost:$requestingPort")
        logger.info("  Scheme: $requestingScheme")
        logger.info("  UserName: $userName")
        logger.info("  Password: <hidden>")
        return PasswordAuthentication(userName, userPassword?.toCharArray())
    }
}

/**
 * Adds a network interceptor to the OkHttpClient.Builder that applies the first non-null proxy
 * returned by the [httpProxyProvider] and [httpsProxyProvider] functions.
 *
 * @param logger used for logging
 * @param httpProxyProvider a function that returns the first non-null [NetworkProxy] for an HTTP
 * request
 * @param httpsProxyProvider a function that returns the first non-null [NetworkProxy] for an HTTPS
 * request
 * @return the OkHttpClient.Builder with the proxy interceptor added
 */
@Suppress("ComplexCondition") // Just checking for all proxy properties
fun OkHttpClient.Builder.addProxyIfAvailable(
    logger: PluginLogger,
    httpProxyProvider: (() -> NetworkProxy?) = { createHttpProxy(logger) },
    httpsProxyProvider: (() -> NetworkProxy?) = { createHttpsProxy(logger) },
): OkHttpClient.Builder {
    val httpsProxy by lazy { httpsProxyProvider() }
    val httpProxy by lazy { httpProxyProvider() }
    return this
        .addNetworkInterceptor { chain ->
            val request = chain.request()
            val proxy = chain.connection()?.route()?.proxy ?: Proxy.NO_PROXY
            if (proxy != Proxy.NO_PROXY && proxy.type() != Proxy.Type.DIRECT) {
                logger.info(requestingProxyMessage(proxy, request))
            } else {
                logger.info(requestingWithoutProxyMessage(request))
            }
            chain.proceed(request)
        }
        .proxySelector(DynamicProxySelector(logger, httpProxyProvider, httpsProxyProvider))
        .proxyAuthenticator { _, response ->
            val url = response.request.url
            val proxyUser =
                if (url.scheme == "https") {
                    httpsProxy?.user
                } else {
                    httpProxy?.user
                }
            val proxyPassword =
                if (url.scheme == "https") {
                    httpsProxy?.password
                } else {
                    httpProxy?.password
                }

            if (proxyUser == null || proxyPassword == null) {
                throw GradleException(proxyCredsNotSpecified())
            }
            val credentials = Credentials.basic(proxyUser, proxyPassword)
            logger.info(applyProxyAuthMessage(proxyUser))
            response.request.newBuilder()
                .header("Proxy-Authorization", credentials)
                .build()
        }
}

/**
 * Dynamic proxy selector that applies the first non-null proxy returned by the [httpProxyProvider] and [httpsProxyProvider] functions.
 *
 * @param logger used for logging
 * @param httpProxyProvider a function that returns the first non-null [NetworkProxy] for an HTTP request
 * @param httpsProxyProvider a function that returns the first non-null [NetworkProxy] for an HTTPS request
 */
private class DynamicProxySelector(
    private val logger: PluginLogger,
    private val httpProxyProvider: (() -> NetworkProxy?),
    private val httpsProxyProvider: (() -> NetworkProxy?),
) : ProxySelector() {
    private val httpsProxy by lazy { httpsProxyProvider() }
    private val httpProxy by lazy { httpProxyProvider() }

    override fun select(uri: URI?): List<Proxy> {
        val proxyHost =
            if (uri?.scheme == "https") {
                httpsProxy?.host
            } else {
                httpProxy?.host
            }
        val proxyPort =
            if (uri?.scheme == "https") {
                httpsProxy?.port
            } else {
                httpProxy?.port
            }
        val nonProxyHosts =
            if (uri?.scheme == "https") {
                httpsProxy?.nonProxyHosts
            } else {
                httpProxy?.nonProxyHosts
            }

        if (proxyHost != null && proxyPort != null) {
            val host = uri?.host ?: return listOf(Proxy.NO_PROXY)
            if (nonProxyHosts != null && nonProxyHosts.split("|").any { host.contains(it) }) {
                return listOf(Proxy.NO_PROXY)
            }
            val proxyAddress = InetSocketAddress.createUnresolved(proxyHost, proxyPort.toInt())
            logger.info(returnAndApplyProxyMessage(uri, proxyAddress))
            return listOf(Proxy(Proxy.Type.HTTP, proxyAddress))
        }
        return listOf(Proxy.NO_PROXY)
    }

    override fun connectFailed(
        uri: URI?,
        address: SocketAddress?,
        error: IOException?,
    ) {
        logger.error(proxyConnectionFailedMessage(uri), error)
    }
}

/**
 * Creates a [RequestBody] from the given [value] string.
 *
 * @param value the string to create a [RequestBody] from
 * @return the created [RequestBody]
 */
fun createPartFromString(value: String): RequestBody {
    return value.toRequestBody(MultipartBody.FORM)
}

/**
 * Creates a [NetworkProxy] for HTTPS requests.
 *
 * @param logger the logger used for logging
 * @return the created [NetworkProxy] for HTTPS requests, or `null` if the proxy cannot be created
 */
fun createHttpsProxy(logger: PluginLogger): NetworkProxy? {
    val host = getEnvOrProperty("https.proxyHost")
    val port = getEnvOrProperty("https.proxyPort")
    return if (host != null && port != null) {
        logger.info(createHttpsProxyMessage(host, port))
        NetworkProxy(
            host = host,
            port = port,
            user = getEnvOrProperty("https.proxyUser"),
            password = getEnvOrProperty("https.proxyPassword"),
            nonProxyHosts = getEnvOrProperty("https.nonProxyHosts"),
        )
    } else {
        logger.info(cannotCreateHttpsProxyMessage(host, port))
        null
    }
}

/**
 * Creates a [NetworkProxy] for HTTP requests.
 *
 * @param logger the logger used for logging
 * @return the created [NetworkProxy] for HTTP requests, or `null` if the proxy cannot be created
 */
fun createHttpProxy(logger: PluginLogger): NetworkProxy? {
    val host = getEnvOrProperty("http.proxyHost")?.takeIf { it.isNotBlank() }
    val port = getEnvOrProperty("http.proxyPort")?.takeIf { it.isNotBlank() }
    return if (host != null && port != null) {
        logger.info(createHttpProxyMessage(host, port))
        NetworkProxy(
            host = host,
            port = port,
            user = getEnvOrProperty("http.proxyUser")?.takeIf { it.isNotBlank() },
            password = getEnvOrProperty("http.proxyPassword")?.takeIf { it.isNotBlank() },
            nonProxyHosts = getEnvOrProperty("http.nonProxyHosts")?.takeIf { it.isNotBlank() },
        )
    } else {
        logger.info(cannotCreateHttpProxyMessage(host, port))
        null
    }
}

/**
 * Retrieves the value of the environment variable or system property with the given [key].
 *
 * @param key the key of the environment variable or system property to retrieve
 * @return the value of the environment variable or system property with the given [key], or `null` if it is not set
 */
fun getEnvOrProperty(key: String): String? {
    return System.getenv(key) ?: System.getProperty(key)
}

/**
 * Data class representing a network proxy.
 */
data class NetworkProxy(
    /**
     * The host of the network proxy.
     */
    val host: String,
    /**
     * The port of the network proxy.
     */
    val port: String,
    /**
     * The username for the network proxy authentication, if any.
     */
    val user: String?,
    /**
     * The password for the network proxy authentication, if any.
     */
    val password: String?,
    /**
     * The non proxy hosts for the network proxy.
     *
     * Non proxy hosts are a list of hosts that should not be proxied.
     * The format of the string is a comma separated list of hosts.
     *
     * Example: "localhost,127.0.0.1,example.com"
     */
    val nonProxyHosts: String? = null,
)
