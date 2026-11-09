package ru.kode.android.build.publish.plugin.yandexdisk.service

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.yandexdisk.controller.YandexDiskController
import ru.kode.android.build.publish.plugin.yandexdisk.controller.entity.YandexDiskShare
import ru.kode.android.build.publish.plugin.yandexdisk.controller.factory.YandexDiskControllerFactory
import ru.kode.android.gradle.commons.logger.LoggerService
import java.io.File
import javax.inject.Inject

abstract class YandexDiskService
    @Inject
    constructor() : BuildService<YandexDiskService.Params> {
        interface Params : BuildServiceParameters {
            val oauthTokenFile: RegularFileProperty
            val loggerService: Property<LoggerService>
        }

        private val controller: YandexDiskController by lazy {
            YandexDiskControllerFactory.build(
                oauthToken =
                    parameters.oauthTokenFile
                        .get()
                        .asFile
                        .readText()
                        .trim(),
                logger = parameters.loggerService.get().logger,
            )
        }

        fun uploadFile(
            remotePath: String,
            remoteFileName: String,
            file: File,
        ) {
            controller.uploadFile(remotePath, remoteFileName, file)
        }

        fun publishFile(
            remotePath: String,
            remoteFileName: String,
        ): YandexDiskShare = controller.publishFile(remotePath, remoteFileName)

        fun resolveRemoteFilePath(
            remotePath: String,
            fileName: String,
        ): String = controller.resolveRemoteFilePath(remotePath, fileName)
    }
