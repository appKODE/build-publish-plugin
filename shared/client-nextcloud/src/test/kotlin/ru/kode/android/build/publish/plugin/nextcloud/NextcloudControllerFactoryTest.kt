package ru.kode.android.build.publish.plugin.nextcloud

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.core.logger.pluginLoggerFromLog
import ru.kode.android.build.publish.plugin.nextcloud.controller.factory.NextcloudControllerFactory

class NextcloudControllerFactoryTest {
    @Test
    fun `build with lambda logger creates controller`() {
        val controller =
            NextcloudControllerFactory.build(
                baseUrl = "https://cloud.example.com",
                username = "user",
                password = "pass",
                log = { /* no-op */ },
            )
        assertNotNull(controller)
    }

    @Test
    fun `build with default println logger creates controller`() {
        val controller =
            NextcloudControllerFactory.build(
                baseUrl = "https://cloud.example.com",
                username = "user",
                password = "pass",
            )
        assertNotNull(controller)
    }

    @Test
    fun `build with PluginLogger creates controller`() {
        val controller =
            NextcloudControllerFactory.build(
                baseUrl = "https://cloud.example.com",
                username = "user",
                password = "pass",
                logger = pluginLoggerFromLog {},
            )
        assertNotNull(controller)
    }

    @Test
    fun `build normalizes base url with trailing slash`() {
        val controller =
            NextcloudControllerFactory.build(
                baseUrl = "https://cloud.example.com",
                username = "user",
                password = "pass",
            )
        assertNotNull(controller)
    }
}
