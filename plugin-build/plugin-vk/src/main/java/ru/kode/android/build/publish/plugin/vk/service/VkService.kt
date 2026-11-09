package ru.kode.android.build.publish.plugin.vk.service

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.vk.controller.VkController
import ru.kode.android.build.publish.plugin.vk.controller.entity.VkSendResult
import ru.kode.android.build.publish.plugin.vk.controller.factory.VkControllerFactory
import ru.kode.android.gradle.commons.logger.LoggerService
import java.io.File
import javax.inject.Inject

abstract class VkService
    @Inject
    constructor() : BuildService<VkService.Params> {
        interface Params : BuildServiceParameters {
            val accessTokenFile: RegularFileProperty
            val loggerService: Property<LoggerService>
        }

        private val controller: VkController by lazy {
            VkControllerFactory.build(
                accessToken =
                    parameters.accessTokenFile
                        .get()
                        .asFile
                        .readText()
                        .trim(),
                logger = parameters.loggerService.get().logger,
            )
        }

        fun sendDocument(
            peerId: Long,
            file: File,
            title: String,
            message: String?,
        ): VkSendResult = controller.sendDocument(peerId, file, title, message)
    }
