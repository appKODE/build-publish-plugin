package ru.kode.android.build.publish.plugin.firebase.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input

interface FirebaseDistributionConfig {
    val name: String

    /**
     * The path to your service account private key JSON file for Firebase App Distribution
     */
    @get:Input
    val serviceCredentialsFilePath: Property<String>

    /**
     * Artifact type for app distribution
     * For example: APK or AAB
     */
    @get:Input
    val artifactType: Property<String>

    /**
     * Custom app id for Firebase App Distribution to override google-services.json
     */
    @get:Input
    val appId: Property<String>

    /**
     * Test groups for app distribution
     *
     * For example: [android-testers]
     */
    @get:Input
    val testerGroups: SetProperty<String>
}
