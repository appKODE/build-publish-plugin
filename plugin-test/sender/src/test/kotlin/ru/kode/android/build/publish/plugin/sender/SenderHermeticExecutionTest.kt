package ru.kode.android.build.publish.plugin.sender

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File

/**
 * Hermetic execution tests for the sender tasks. Each starts an in-process [MockWebServer], points the
 * sender block's base URL at it, runs the real task through GradleRunner (which reaches the server over
 * localhost), and asserts the recorded HTTP request. No live credentials, no network — runs on any CI OS.
 */
class SenderHermeticExecutionTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var server: MockWebServer

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `sendSlackMessage posts the message to the configured webhook`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val projectDir = File(tempDir, "test-slack")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.sender",
            pluginConfigBlock =
                """
                buildPublishSender {
                    slack {
                        webhookUrl.set("${server.url("/services/webhook")}")
                    }
                }
                """.trimIndent(),
        )

        val result =
            projectDir.runTask(
                "sendSlackMessage",
                cliArguments = listOf("--message", "Hello hermetic world"),
            )

        result.outputShouldContain("BUILD SUCCESSFUL")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/services/webhook", request.path)
        assertTrue(
            request.body.readUtf8().contains("Hello hermetic world"),
            "Slack payload should contain the message text",
        )
    }

    @Test
    fun `sendTelegramMessage posts the message to the configured bot server`() {
        // The message may be split into chunks, so answer every POST with a 200.
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return MockResponse().setResponseCode(200).setBody("")
                }
            }
        val projectDir = File(tempDir, "test-telegram")
        val baseUrl = server.url("/").toString().trimEnd('/')
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.sender",
            pluginConfigBlock =
                """
                buildPublishSender {
                    telegram {
                        botId.set("123456:FAKE_TOKEN")
                        chatId.set("-1001234567890")
                        serverBaseUrl.set("$baseUrl")
                    }
                }
                """.trimIndent(),
        )

        val result =
            projectDir.runTask(
                "sendTelegramMessage",
                cliArguments = listOf("--message", "Hello hermetic telegram"),
            )

        result.outputShouldContain("BUILD SUCCESSFUL")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(
            request.path?.contains("/bot123456:FAKE_TOKEN/sendMessage") == true,
            "Telegram send should target the bot sendMessage endpoint, was ${request.path}",
        )
        assertTrue(
            request.body.readUtf8().contains("Hello hermetic telegram"),
            "Telegram payload should contain the message text",
        )
    }
}
