package ru.kode.android.build.publish.plugin.clickup

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.clickup.controller.factory.ClickUpControllerFactory
import ru.kode.android.build.publish.plugin.core.logger.pluginLoggerFromLog

class ClickUpControllerFactoryTest {
    @Test
    fun `build with lambda logger creates controller`() {
        val controller =
            ClickUpControllerFactory.build(
                token = "fake-api-token",
                log = { /* no-op */ },
            )
        assertNotNull(controller)
    }

    @Test
    fun `build with default println logger creates controller`() {
        val controller = ClickUpControllerFactory.build(token = "fake-api-token")
        assertNotNull(controller)
    }

    @Test
    fun `build with PluginLogger creates controller`() {
        val controller =
            ClickUpControllerFactory.build(
                token = "fake-api-token",
                logger = pluginLoggerFromLog {},
            )
        assertNotNull(controller)
    }

    @Test
    fun `build does not throw when creating controller`() {
        val controller = ClickUpControllerFactory.build(token = "another-token")
        assertNotNull(controller)
    }
}
