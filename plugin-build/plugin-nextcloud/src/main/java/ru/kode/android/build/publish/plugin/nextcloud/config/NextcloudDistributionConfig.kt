package ru.kode.android.build.publish.plugin.nextcloud.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Configuration for publishing Android artifacts to Nextcloud.
 */
interface NextcloudDistributionConfig {
    val name: String

    /**
     * Target folder under `/remote.php/dav/files/{username}/`.
     *
     * Example: `mobile/project-a/release`
     */
    @get:Input
    val remotePath: Property<String>

    /**
     * Whether to compress the distribution file before sending.
     *
     * Default: `false`
     */
    @get:Input
    @get:Optional
    val compressed: Property<Boolean>

    /**
     * Sharing mode used after upload.
     *
     * Default: [NextcloudShareMode.INTERNAL_RECIPIENTS]
     */
    @get:Input
    @get:Optional
    val shareMode: Property<NextcloudShareMode>

    /**
     * Nextcloud user recipients for internal sharing (`shareType=0`).
     */
    @get:Input
    @get:Optional
    val userRecipients: SetProperty<String>

    /**
     * Nextcloud group recipients for internal sharing (`shareType=1`).
     */
    @get:Input
    @get:Optional
    val groupRecipients: SetProperty<String>

    /**
     * Optional explicit target file name on remote storage.
     *
     * If absent, plugin computes deterministic version-based names.
     */
    @get:Input
    @get:Optional
    val remoteFileName: Property<String>

    fun userRecipient(value: String) {
        userRecipients.add(value)
    }

    fun userRecipient(value: Provider<String>) {
        userRecipients.add(value)
    }

    fun userRecipients(vararg values: String) {
        userRecipients.addAll(values.toList())
    }

    fun userRecipients(values: Iterable<String>) {
        userRecipients.addAll(values)
    }

    fun userRecipients(values: Provider<Iterable<String>>) {
        userRecipients.addAll(values)
    }

    fun groupRecipient(value: String) {
        groupRecipients.add(value)
    }

    fun groupRecipient(value: Provider<String>) {
        groupRecipients.add(value)
    }

    fun groupRecipients(vararg values: String) {
        groupRecipients.addAll(values.toList())
    }

    fun groupRecipients(values: Iterable<String>) {
        groupRecipients.addAll(values)
    }

    fun groupRecipients(values: Provider<Iterable<String>>) {
        groupRecipients.addAll(values)
    }
}
