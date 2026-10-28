package ru.kode.android.build.publish.plugin.firebase.controller

import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigTemplate
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigVersion
import ru.kode.android.build.publish.plugin.firebase.network.api.FirebaseRemoteConfigApi
import ru.kode.android.gradle.commons.util.executeWithResult
import java.time.Instant

internal const val FIREBASE_REMOTE_CONFIG_BASE_URL = "https://firebaseremoteconfig.googleapis.com"
private const val MAX_PAGE_SIZE = 300

internal class FirebaseRemoteConfigControllerImpl(
    private val api: FirebaseRemoteConfigApi,
    private val baseUrl: String,
) : FirebaseRemoteConfigController {
    override fun getTemplate(
        projectId: String,
        versionNumber: String?,
    ): RemoteConfigTemplate = api.getTemplate(templateUrl(projectId), versionNumber).executeWithResult().getOrThrow()

    override fun listVersions(
        projectId: String,
        startTime: Instant,
    ): List<RemoteConfigVersion> {
        val url = "${templateUrl(projectId)}:listVersions"
        val versions = mutableListOf<RemoteConfigVersion>()
        var pageToken: String? = null
        do {
            val page =
                api.listVersions(url, startTime.toString(), MAX_PAGE_SIZE, pageToken)
                    .executeWithResult()
                    .getOrThrow()
            versions += page.versions
            pageToken = page.nextPageToken?.takeIf { it.isNotEmpty() }
        } while (pageToken != null)
        return versions
    }

    private fun templateUrl(projectId: String) = "${baseUrl.trimEnd('/')}/v1/projects/$projectId/remoteConfig"
}
