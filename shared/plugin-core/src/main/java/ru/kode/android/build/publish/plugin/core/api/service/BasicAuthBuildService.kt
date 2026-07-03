package ru.kode.android.build.publish.plugin.core.api.service

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import javax.inject.Inject

abstract class BasicAuthBuildService<C>
    @Inject
    constructor() : BuildService<BasicAuthBuildService.Params> {
        interface Params : BuildServiceParameters {
            val baseUrl: Property<String>
            val credentials: Property<BasicAuthCredentials>
            val loggerService: Property<LoggerService>
        }

        protected abstract fun buildController(
            baseUrl: String,
            username: String,
            password: String,
            logger: PluginLogger,
        ): C

        protected val controller: C by lazy {
            buildController(
                baseUrl = parameters.baseUrl.get(),
                username = parameters.credentials.get().username.get(),
                password = parameters.credentials.get().password.get(),
                logger = parameters.loggerService.get().logger,
            )
        }
    }
