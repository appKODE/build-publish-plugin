package ru.kode.android.build.publish.plugin.confluence

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.confluence.controller.factory.ConfluenceControllerFactory
import ru.kode.android.build.publish.plugin.core.logger.pluginLoggerFromLog

class ConfluenceControllerFactoryTest {
    @Test
    fun `build with lambda logger creates controller`() {
        val controller =
            ConfluenceControllerFactory.build(
                baseUrl = "https://confluence.example.com",
                username = "user",
                password = "pass",
                log = { /* no-op */ },
            )
        assertNotNull(controller)
    }

    @Test
    fun `build with default println logger creates controller`() {
        val controller =
            ConfluenceControllerFactory.build(
                baseUrl = "https://confluence.example.com",
                username = "user",
                password = "pass",
            )
        assertNotNull(controller)
    }

    @Test
    fun `build with PluginLogger creates controller`() {
        val controller =
            ConfluenceControllerFactory.build(
                baseUrl = "https://confluence.example.com",
                username = "user",
                password = "pass",
                logger = pluginLoggerFromLog {},
            )
        assertNotNull(controller)
    }

    @Test
    fun `build does not throw when creating controller`() {
        val controller =
            ConfluenceControllerFactory.build(
                baseUrl = "https://confluence.example.com",
                username = "admin",
                password = "secret",
            )
        assertNotNull(controller)
    }
}
