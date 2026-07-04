package ru.kode.android.build.publish.plugin.nextcloud.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import ru.kode.android.build.publish.plugin.core.util.CollectionStrategy
import ru.kode.android.build.publish.plugin.core.util.CollectionStrategy.REPLACE
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.inheritFrom

/**
 * Configuration for publishing Android artifacts to Nextcloud.
 */
interface NextcloudDistributionConfig : CommonConfigMergeable<NextcloudDistributionConfig> {
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

    /** Merge strategy for [userRecipients]; configuration-time only, defaults to REPLACE. */
    @get:Internal
    val userRecipientsStrategy: Property<CollectionStrategy>

    /** Merge strategy for [groupRecipients]; configuration-time only, defaults to REPLACE. */
    @get:Internal
    val groupRecipientsStrategy: Property<CollectionStrategy>

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

    /**
     * Adds user recipients and selects how this collection is merged with the common configuration
     * for a per-version-name build ([CollectionStrategy.REPLACE] by default).
     */
    fun userRecipients(
        strategy: CollectionStrategy,
        vararg values: String,
    ) {
        userRecipientsStrategy.set(strategy)
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

    /**
     * Adds group recipients and selects how this collection is merged with the common configuration
     * for a per-version-name build ([CollectionStrategy.REPLACE] by default).
     */
    fun groupRecipients(
        strategy: CollectionStrategy,
        vararg values: String,
    ) {
        groupRecipientsStrategy.set(strategy)
        groupRecipients.addAll(values.toList())
    }

    fun groupRecipients(values: Iterable<String>) {
        groupRecipients.addAll(values)
    }

    fun groupRecipients(values: Provider<Iterable<String>>) {
        groupRecipients.addAll(values)
    }

    override fun inheritFrom(common: NextcloudDistributionConfig) {
        remotePath.convention(common.remotePath)
        compressed.convention(common.compressed)
        shareMode.convention(common.shareMode)
        remoteFileName.convention(common.remoteFileName)
        userRecipients.inheritFrom(common.userRecipients, userRecipientsStrategy.getOrElse(REPLACE))
        groupRecipients.inheritFrom(common.groupRecipients, groupRecipientsStrategy.getOrElse(REPLACE))
    }
}
