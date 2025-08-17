package ru.kode.android.build.publish.plugin.firebase.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input

/**
 * Configuration class for Firebase App Distribution settings.
 *
 * This class defines the configuration options for distributing Android applications
 * through Firebase App Distribution. It allows customization of distribution settings
 * such as service account credentials, artifact type, and tester groups.
 */
abstract class FirebaseDistributionConfig {
    abstract val name: String

    /**
     * The file system path to the service account credentials JSON file.
     *
     * This file contains the private key for authenticating with Firebase App Distribution.
     * You can generate this file in the Firebase Console under Project Settings > Service Accounts.
     *
     * Example: `"path/to/service-account.json"`
     */
    @get:Input
    abstract val serviceCredentialsFilePath: Property<String>

    /**
     * The type of Android application artifact to distribute.
     *
     * Supported values:
     * - `"APK"`: For Android application packages
     * - `"AAB"`: For Android App Bundles (recommended for production)
     *
     * Default: None (must be specified)
     */
    @get:Input
    abstract val artifactType: Property<String>

    /**
     * The Firebase App ID for distribution.
     *
     * This overrides the app ID from google-services.json if specified.
     * You can find this ID in the Firebase Console under Project Settings > General.
     *
     * Format: `1:1234567890:android:0123456789abcdef`
     */
    @get:Input
    abstract val appId: Property<String>

    /**
     * The set of tester groups that should receive this distribution.
     *
     * Tester groups must be created in the Firebase Console before they can be used here.
     */
    @get:Input
    internal abstract val testerGroups: SetProperty<String>

    /**
     * Adds a single tester group to receive this distribution.
     *
     * @param testerGroup The name of the tester group to add
     * @see testerGroup
     */
    fun testerGroup(testerGroup: String) {
        testerGroups.add(testerGroup)
    }

    /**
     * Adds multiple tester groups to receive this distribution.
     *
     * @param testerGroup Vararg parameter of tester group names to add
     * @see testerGroup
     */
    fun testerGroups(vararg testerGroup: String) {
        testerGroups.addAll(testerGroup.toList())
    }
}
