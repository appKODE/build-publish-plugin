package ru.kode.android.build.publish.plugin.telegram

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.core.logger.pluginLoggerFromLog
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramControllerFactory

class TelegramControllerFactoryTest {
    @Test
    fun `build with lambda logger creates controller`() {
        val controller = TelegramControllerFactory.build { /* no-op */ }
        assertNotNull(controller)
    }

    @Test
    fun `build with default println logger creates controller`() {
        val controller = TelegramControllerFactory.build()
        assertNotNull(controller)
    }

    @Test
    fun `build with PluginLogger creates controller`() {
        val logger = pluginLoggerFromLog {}
        val controller = TelegramControllerFactory.build(logger)
        assertNotNull(controller)
    }
}
