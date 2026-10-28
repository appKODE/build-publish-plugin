package ru.kode.android.build.publish.plugin.firebase.controller

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.create
import ru.kode.android.build.publish.plugin.firebase.network.api.FirebaseRemoteConfigApi
import ru.kode.android.build.publish.plugin.firebase.network.entity.ServiceAccountKey
import ru.kode.android.build.publish.plugin.firebase.network.factory.FirebaseRemoteConfigClientFactory
import ru.kode.android.gradle.commons.logger.PluginLogger
import java.io.File

private const val STUB_BASE_URL = "http://localhost/"

object FirebaseRemoteConfigControllerFactory {
    /**
     * @param serviceCredentialsFile Google service account JSON key.
     * @param apiBaseUrl Remote Config API host, `null` for the production one; overridable for tests.
     */
    fun build(
        serviceCredentialsFile: File,
        logger: PluginLogger,
        apiBaseUrl: String? = null,
    ): FirebaseRemoteConfigController {
        val json = Json { ignoreUnknownKeys = true }
        val key = json.decodeFromString<ServiceAccountKey>(serviceCredentialsFile.readText())
        val api =
            Retrofit.Builder()
                .baseUrl(STUB_BASE_URL)
                .client(FirebaseRemoteConfigClientFactory.build(key, logger, json))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create<FirebaseRemoteConfigApi>()
        return FirebaseRemoteConfigControllerImpl(api, apiBaseUrl ?: FIREBASE_REMOTE_CONFIG_BASE_URL)
    }
}
