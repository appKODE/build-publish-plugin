package ru.kode.android.build.publish.plugin.jira

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.core.logger.pluginLoggerFromLog
import ru.kode.android.build.publish.plugin.jira.controller.factory.JiraControllerFactory

class JiraControllerFactoryTest {
    @Test
    fun `build with lambda logger creates controller`() {
        val controller =
            JiraControllerFactory.build(
                baseUrl = "https://jira.example.com",
                username = "user",
                password = "pass",
                log = { /* no-op */ },
            )
        assertNotNull(controller)
    }

    @Test
    fun `build with default println logger creates controller`() {
        val controller =
            JiraControllerFactory.build(
                baseUrl = "https://jira.example.com",
                username = "user",
                password = "pass",
            )
        assertNotNull(controller)
    }

    @Test
    fun `build with PluginLogger creates controller`() {
        val controller =
            JiraControllerFactory.build(
                baseUrl = "https://jira.example.com",
                username = "user",
                password = "pass",
                logger = pluginLoggerFromLog {},
            )
        assertNotNull(controller)
    }

    @Test
    fun `build does not throw when creating controller`() {
        val controller =
            JiraControllerFactory.build(
                baseUrl = "https://jira.example.com",
                username = "admin",
                password = "secret",
            )
        assertNotNull(controller)
    }
}
