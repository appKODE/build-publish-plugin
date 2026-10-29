package ru.kode.android.build.publish.plugin.firebase.network.factory

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import ru.kode.android.build.publish.plugin.firebase.network.entity.ServiceAccountKey
import ru.kode.android.build.publish.plugin.firebase.network.entity.TokenResponse
import java.io.IOException
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

private const val SCOPE = "https://www.googleapis.com/auth/firebase.remoteconfig"
private const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"
private const val ASSERTION_LIFETIME_SECONDS = 3600L
private const val TOKEN_REFRESH_MARGIN_SECONDS = 60L

/**
 * Adds `Authorization: Bearer <token>` using the OAuth 2.0 JWT bearer flow for service accounts
 * (RFC 7523, the same flow `GoogleCredentials` uses).
 *
 * Implemented with the JDK and OkHttp instead of `google-auth-library` so the token request goes
 * through the same proxy settings as the API calls, and to keep `google-http-client` off the
 * plugin classpath where it clashes with other Google libraries.
 *
 * [tokenClient] must not log bodies: the token response contains the access token.
 */
internal class ServiceAccountTokenInterceptor(
    private val key: ServiceAccountKey,
    private val tokenClient: OkHttpClient,
    private val json: Json,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000 },
) : Interceptor {
    private var token: String? = null
    private var expiresAt = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain.request().newBuilder()
                .header("Authorization", "Bearer ${accessToken()}")
                .build()
        return chain.proceed(request)
    }

    @Synchronized
    private fun accessToken(): String {
        val now = clock()
        token?.takeIf { now < expiresAt - TOKEN_REFRESH_MARGIN_SECONDS }?.let { return it }
        val body =
            FormBody.Builder()
                .add("grant_type", GRANT_TYPE)
                .add("assertion", signedAssertion(now))
                .build()
        val request = Request.Builder().url(key.tokenUri).post(body).build()
        val response =
            tokenClient.newCall(request).execute().use { response ->
                val text = response.body.string()
                if (!response.isSuccessful) {
                    throw IOException("Failed to obtain Google access token: HTTP ${response.code} $text")
                }
                json.decodeFromString<TokenResponse>(text)
            }
        token = response.accessToken
        expiresAt = now + response.expiresIn
        return response.accessToken
    }

    private fun signedAssertion(now: Long): String {
        val header =
            buildJsonObject {
                put("alg", "RS256")
                put("typ", "JWT")
            }
        val claims =
            buildJsonObject {
                put("iss", key.clientEmail)
                put("scope", SCOPE)
                put("aud", key.tokenUri)
                put("iat", now)
                put("exp", now + ASSERTION_LIFETIME_SECONDS)
            }
        val unsigned = "${header.toString().base64Url()}.${claims.toString().base64Url()}"
        val signature =
            Signature.getInstance("SHA256withRSA").run {
                initSign(KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(key.privateKeyBytes())))
                update(unsigned.toByteArray())
                sign()
            }
        return "$unsigned.${base64Url(signature)}"
    }
}

private fun ServiceAccountKey.privateKeyBytes(): ByteArray =
    Base64.getMimeDecoder().decode(
        privateKey
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", ""),
    )

private fun String.base64Url(): String = base64Url(toByteArray())

private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
