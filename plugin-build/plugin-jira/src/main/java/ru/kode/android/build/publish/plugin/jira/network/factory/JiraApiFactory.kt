package ru.kode.android.build.publish.plugin.jira.network.factory

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.jira.network.api.JiraApi

internal object JiraApiFactory {

    fun build(client: OkHttpClient, baseUrl: String): JiraApi {
        val moshi = Moshi.Builder().build()
        return Retrofit.Builder()
            .baseUrl("$baseUrl/rest/api/2/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JiraApi::class.java)
    }
}