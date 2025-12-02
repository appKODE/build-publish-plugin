package ru.kode.android.build.publish.plugin.jira.controller.factory

import ru.kode.android.build.publish.plugin.jira.controller.JiraController
import ru.kode.android.build.publish.plugin.jira.controller.JiraControllerImpl
import ru.kode.android.build.publish.plugin.jira.network.factory.JiraApiFactory
import ru.kode.android.build.publish.plugin.jira.network.factory.JiraClientFactory

object JiraControllerFactory {

    fun build(baseUrl: String, username: String, password: String): JiraController {
        return JiraControllerImpl(
            api = JiraApiFactory.build(
                client = JiraClientFactory.build(
                    username,
                    password
                ),
                baseUrl = baseUrl
            )
        )
    }
}
