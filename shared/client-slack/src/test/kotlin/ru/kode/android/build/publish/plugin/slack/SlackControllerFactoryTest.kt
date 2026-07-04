package ru.kode.android.build.publish.plugin.slack

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.core.logger.pluginLoggerFromLog
import ru.kode.android.build.publish.plugin.slack.controller.SlackControllerFactory

class SlackControllerFactoryTest {
    @Test
    fun `build with lambda logger creates controller`() {
        val controller = SlackControllerFactory.build { /* no-op */ }
        assertNotNull(controller)
    }

    @Test
    fun `build with default println logger creates controller`() {
        val controller = SlackControllerFactory.build()
        assertNotNull(controller)
    }

    @Test
    fun `build with PluginLogger creates controller`() {
        val logger = pluginLoggerFromLog { /* no-op */ }
        val controller = SlackControllerFactory.build(logger)
        assertNotNull(controller)
    }
}
