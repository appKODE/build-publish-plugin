package ru.kode.android.build.publish.plugin.nextcloud.service

import okhttp3.OkHttpClient
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudShareMode
import ru.kode.android.build.publish.plugin.nextcloud.controller.NextcloudController
import ru.kode.android.build.publish.plugin.nextcloud.controller.NextcloudControllerImpl
import ru.kode.android.build.publish.plugin.nextcloud.controller.entity.NextcloudSharingResult
import ru.kode.android.build.publish.plugin.nextcloud.network.api.NextcloudApi
import ru.kode.android.build.publish.plugin.nextcloud.network.factory.NextcloudApiFactory
import ru.kode.android.build.publish.plugin.nextcloud.network.factory.NextcloudClientFactory
import java.io.File
import javax.inject.Inject

abstract class NextcloudService
    @Inject
    constructor() : BuildService<NextcloudService.Params> {
        interface Params : BuildServiceParameters {
            val baseUrl: Property<String>
            val credentials: Property<BasicAuthCredentials>
            val loggerService: Property<LoggerService>
        }

        internal abstract val okHttpClientProperty: Property<OkHttpClient>
        internal abstract val apiProperty: Property<NextcloudApi>
        internal abstract val controllerProperty: Property<NextcloudController>

        init {
            okHttpClientProperty.set(
                parameters.loggerService.map { it.logger }.flatMap { logger ->
                    parameters.credentials.flatMap { it.username }
                        .zip(parameters.credentials.flatMap { it.password }) { username, password ->
                            NextcloudClientFactory.build(username, password, logger)
                        }
                },
            )
            apiProperty.set(
                okHttpClientProperty.zip(parameters.baseUrl) { client, baseUrl ->
                    NextcloudApiFactory.build(client, ensureTrailingSlash(baseUrl))
                },
            )
            controllerProperty.set(
                parameters.credentials
                    .flatMap { it.username }
                    .zip(apiProperty) { username, api ->
                        username to api
                    }
                    .zip(parameters.baseUrl) { (username, api), baseUrl ->
                        NextcloudControllerImpl(
                            baseUrl = ensureTrailingSlash(baseUrl),
                            username = username,
                            api = api,
                        )
                    },
            )
        }

        private val controller: NextcloudController get() = controllerProperty.get()

        fun uploadFile(
            remotePath: String,
            remoteFileName: String,
            file: File,
        ) {
            controller.uploadFile(remotePath, remoteFileName, file)
        }

        fun shareFile(
            remotePath: String,
            remoteFileName: String,
            shareMode: NextcloudShareMode,
            userRecipients: Set<String>,
            groupRecipients: Set<String>,
        ): NextcloudSharingResult {
            return controller.shareFile(
                remotePath = remotePath,
                remoteFileName = remoteFileName,
                shareMode = shareMode,
                userRecipients = userRecipients,
                groupRecipients = groupRecipients,
            )
        }

        fun resolveRemoteFilePath(
            remotePath: String,
            fileName: String,
        ): String {
            return controller.resolveRemoteFilePath(remotePath, fileName)
        }
    }

private fun ensureTrailingSlash(value: String): String {
    return if (value.endsWith('/')) value else "$value/"
}
