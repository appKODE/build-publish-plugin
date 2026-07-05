package ru.kode.android.build.publish.plugin.slack

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.core.entity.IssueSource
import ru.kode.android.build.publish.plugin.slack.controller.SlackControllerFactory
import ru.kode.android.build.publish.plugin.slack.controller.SlackMessage

class SlackControllerImplTest {
    private val server = MockWebServer()
    private val logs = mutableListOf<String>()
    private val controller by lazy { SlackControllerFactory.build { msg -> logs.add(msg) } }

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    @Test
    fun `send posts JSON body to webhook url`() {
        server.enqueue(MockResponse().setResponseCode(200))

        controller.send(
            SlackMessage(
                webhookUrl = server.url("/slack/webhook").toString(),
                text = "Hello World",
                header = "Test Header",
            ),
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        val body = request.body.readUtf8()
        assertTrue(body.contains("Test Header"), "Body should contain header text")
        assertTrue(body.contains("Hello World"), "Body should contain message text")
    }

    @Test
    fun `send uses SlackMessage defaults when optional fields omitted`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val message =
            SlackMessage(
                webhookUrl = server.url("/webhook").toString(),
                text = "minimal",
            )

        assertEquals("", message.header)
        assertEquals(emptyList<String>(), message.userMentions)
        assertEquals("", message.iconUrl)
        assertEquals("#36a64f", message.attachmentColor)
        assertEquals(emptyList<IssueSource>(), message.issueSources)

        controller.send(message)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
    }

    @Test
    fun `send formats issue links from a single issue source`() {
        server.enqueue(MockResponse().setResponseCode(200))

        controller.send(
            SlackMessage(
                webhookUrl = server.url("/webhook").toString(),
                text = "[APP-123] Fixed a bug",
                issueSources =
                    listOf(
                        IssueSource(numberPattern = "APP-\\d+", urlPrefix = "https://jira.example.com/browse/"),
                    ),
            ),
        )

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("https://jira.example.com/browse/APP-123"), "Should contain formatted issue link")
    }

    @Test
    fun `send links each issue source to its own host`() {
        server.enqueue(MockResponse().setResponseCode(200))

        controller.send(
            SlackMessage(
                webhookUrl = server.url("/webhook").toString(),
                text = "[BASE-1] and [LEG-2] shipped",
                issueSources =
                    listOf(
                        IssueSource(numberPattern = "BASE-\\d+", urlPrefix = "https://jira1/browse/"),
                        IssueSource(numberPattern = "LEG-\\d+", urlPrefix = "https://jira2/browse/"),
                    ),
            ),
        )

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("https://jira1/browse/BASE-1"), "BASE key should link to its own host")
        assertTrue(body.contains("https://jira2/browse/LEG-2"), "LEG key should link to its own host")
    }

    @Test
    fun `send leaves keys unlinked when source has no url prefix`() {
        server.enqueue(MockResponse().setResponseCode(200))

        controller.send(
            SlackMessage(
                webhookUrl = server.url("/webhook").toString(),
                text = "[APP-123] Fixed a bug",
                issueSources =
                    listOf(
                        IssueSource(numberPattern = "APP-\\d+", urlPrefix = null),
                    ),
            ),
        )

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("APP-123"), "Issue key should still be present")
        assertTrue(!body.contains("<http"), "No link markup should be produced without a url prefix")
    }
}
