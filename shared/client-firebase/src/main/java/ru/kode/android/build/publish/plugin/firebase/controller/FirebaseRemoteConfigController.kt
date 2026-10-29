package ru.kode.android.build.publish.plugin.firebase.controller

import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigTemplate
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigVersion
import java.time.Instant

/**
 * Read-only access to Firebase Remote Config templates (`roles/cloudconfig.viewer` is enough).
 */
interface FirebaseRemoteConfigController {
    /**
     * Returns the active template, or the template with [versionNumber] when it is set.
     */
    fun getTemplate(
        projectId: String,
        versionNumber: String? = null,
    ): RemoteConfigTemplate

    /**
     * Returns template versions published after [startTime], newest first.
     *
     * Firebase keeps at most 300 versions for at most 90 days, so older history is not available.
     */
    fun listVersions(
        projectId: String,
        startTime: Instant,
    ): List<RemoteConfigVersion>
}
