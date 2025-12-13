package ru.kode.android.build.publish.plugin.confluence.network.factory

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.confluence.network.api.ConfluenceApi

/**
 * Factory for creating instances of [ConfluenceApi].
 */
internal object ConfluenceApiFactory {

    /**
     * Builds an instance of [ConfluenceApi] using the provided [OkHttpClient] and [baseUrl].
     *
     * @param client The [OkHttpClient] to be used for network requests.
     * @param baseUrl The base URL of the Confluence instance.
     * @return A new instance of [ConfluenceApi].
     */
    fun build(client: OkHttpClient, baseUrl: String): ConfluenceApi {
        val moshi = Moshi.Builder().build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ConfluenceApi::class.java)
    }
}