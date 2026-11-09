package ru.kode.android.build.publish.plugin.yandexdisk

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.core.task.registerStandaloneServiceTask
import ru.kode.android.build.publish.plugin.yandexdisk.extension.BuildPublishYandexDiskExtension
import ru.kode.android.build.publish.plugin.yandexdisk.messages.noAuthConfigsMessage
import ru.kode.android.build.publish.plugin.yandexdisk.messages.registeringServicesMessage
import ru.kode.android.build.publish.plugin.yandexdisk.messages.servicesCreatedMessage
import ru.kode.android.build.publish.plugin.yandexdisk.service.YandexDiskService
import ru.kode.android.build.publish.plugin.yandexdisk.service.YandexDiskServiceExtension
import ru.kode.android.build.publish.plugin.yandexdisk.task.standalone.UploadToYandexDiskTask
import ru.kode.android.gradle.commons.logger.LoggerService
import ru.kode.android.gradle.commons.util.applyWithOptionalAndroid
import ru.kode.android.gradle.commons.util.getOrRegisterLoggerService
import ru.kode.android.gradle.commons.util.resolveStandaloneService
import ru.kode.android.gradle.commons.util.serviceName

internal const val EXTENSION_NAME = "buildPublishYandexDisk"
private const val SERVICE_NAME = "yandexdiskService"
private const val SERVICE_EXTENSION_NAME = "yandexdiskServiceExtension"

/**
 * A Gradle plugin that uploads build artifacts and changelogs to Yandex Disk
 * and optionally publishes them with a public link.
 */
abstract class BuildPublishYandexDiskPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                EXTENSION_NAME,
                BuildPublishYandexDiskExtension::class.java,
            )

        val servicesProperty: MapProperty<String, Provider<*>> =
            project.objects.mapProperty(String::class.java, Provider::class.java)
        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            YandexDiskServiceExtension::class.java,
            servicesProperty,
        )

        project.applyWithOptionalAndroid {
            setupServicesAndTasks(project, extension, servicesProperty)
        }
    }

    private fun setupServicesAndTasks(
        project: Project,
        extension: BuildPublishYandexDiskExtension,
        servicesProperty: MapProperty<String, Provider<*>>,
    ) {
        val loggerProvider = project.getOrRegisterLoggerService()
        val logger = loggerProvider.get()

        if (extension.auth.isEmpty()) {
            logger.info(noAuthConfigsMessage())
            return
        }

        logger.info(registeringServicesMessage())

        val serviceMap =
            extension.auth.associate { authConfig ->
                val name = authConfig.name

                val service =
                    project.gradle.sharedServices.registerIfAbsent(
                        project.serviceName(SERVICE_NAME, name),
                        YandexDiskService::class.java,
                    ) {
                        it.maxParallelUsages.set(1)
                        it.parameters.oauthTokenFile.set(authConfig.oauthTokenFile)
                        it.parameters.loggerService.set(loggerProvider)
                    }

                name to (service as Provider<*>)
            }

        logger.info(servicesCreatedMessage(serviceMap.keys))
        servicesProperty.set(serviceMap)

        val standaloneService = serviceMap.resolveStandaloneService<YandexDiskService>()
        registerStandaloneTasks(project, standaloneService, loggerProvider)
    }

    private fun registerStandaloneTasks(
        project: Project,
        service: Provider<YandexDiskService>,
        loggerProvider: Provider<LoggerService>,
    ) {
        project.registerStandaloneServiceTask<UploadToYandexDiskTask, YandexDiskService>(
            TaskNames.YandexDisk.UPLOAD,
            service,
            loggerProvider,
        )
    }
}
