@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.slack

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.slack.extension.BuildPublishSlackExtension
import ru.kode.android.build.publish.plugin.slack.service.SlackServiceExtension
import ru.kode.android.build.publish.plugin.slack.service.network.SlackNetworkService
import ru.kode.android.build.publish.plugin.slack.service.upload.SlackUploadService

private const val EXTENSION_NAME = "buildPublishSlack"
private const val NETWORK_SERVICE_NAME = "slackNetworkService"
private const val UPLOAD_SERVICE_NAME = "slackUploadService"
private const val SERVICE_EXTENSION_NAME = "slackServiceExtension"

/**
 * A Gradle plugin that provides Slack integration for build publishing.
 *
 * This plugin enables:
 * - Uploading build artifacts to Slack with rich text changelog
 * - Configuring multiple Slack workspaces and channels
 * - Customizing notification messages with user mentions and version info
 *
 * It sets up the necessary services for Slack file upload functionality,
 * and provides extensions for build scripts to configure Slack integration.
 */
abstract class BuildPublishSlackPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, BuildPublishSlackExtension::class.java)

        val androidExtension =
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

        val networkService =
            project.gradle.sharedServices.registerIfAbsent(
                NETWORK_SERVICE_NAME,
                SlackNetworkService::class.java,
                { it.maxParallelUsages.set(1) },
            )

        androidExtension.finalizeDsl {
            val uploadServices: Provider<Map<String, Provider<SlackUploadService>>> =
                project.provider {
                    extension.distribution.fold(mapOf()) { acc, authConfig ->
                        val uploadService =
                            project.gradle.sharedServices.registerIfAbsent(
                                project.serviceName(UPLOAD_SERVICE_NAME, authConfig.name),
                                SlackUploadService::class.java,
                                {
                                    it.maxParallelUsages.set(1)
                                    it.parameters.uploadApiTokenFile.set(authConfig.uploadApiTokenFile)
                                    it.parameters.networkService.set(networkService)
                                },
                            )
                        acc.toMutableMap().apply {
                            put(authConfig.name, uploadService)
                        }
                    }
                }
            project.extensions.create(
                SERVICE_EXTENSION_NAME,
                SlackServiceExtension::class.java,
                uploadServices,
            )
        }
    }
}
