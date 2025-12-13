package ru.kode.android.build.publish.plugin.jira.controller.factory

import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.jira.controller.JiraController
import ru.kode.android.build.publish.plugin.jira.controller.JiraControllerImpl
import ru.kode.android.build.publish.plugin.jira.network.factory.JiraApiFactory
import ru.kode.android.build.publish.plugin.jira.network.factory.JiraClientFactory

/**
 * Factory for creating instances of [JiraController].
 */
object JiraControllerFactory {

    /**
     * Creates an instance of [JiraController].
     *
     * @param baseUrl the base URL of the Jira instance
     * @param username the username for authentication
     * @param password the password for authentication
     * @param logger the logger for logging
     * @return an instance of [JiraController]
     */
    fun build(
        baseUrl: String,
        username: String,
        password: String,
        logger: Logger,
    ): JiraController {
        return JiraControllerImpl(
            api = JiraApiFactory.build(
                client = JiraClientFactory.build(
                    username,
                    password,
                    logger
                ),
                baseUrl = baseUrl
            ),
            logger = logger
        )
    }
}
