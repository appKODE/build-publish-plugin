package ru.kode.android.build.publish.plugin.telegram

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.core.enity.IssueSource
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramControllerFactory
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramMessage
import ru.kode.android.build.publish.plugin.telegram.controller.entity.ChatSpecificTelegramBot

class TelegramControllerImplTest {
    private val server = MockWebServer()
    private val logs = mutableListOf<String>()

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    private fun makeBot(chatId: String = "-100123456789"): ChatSpecificTelegramBot {
        val baseUrl = server.url("/").toString().trimEnd('/')
        return ChatSpecificTelegramBot(
            name = "test-bot",
            id = "123456:FAKE_TOKEN",
            serverBaseUrl = baseUrl,
            basicAuth = null,
            chatId = chatId,
            topicId = null,
        )
    }

    @Test
    fun `send posts message to Telegram API`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))

        val bot = makeBot()
        val controller = TelegramControllerFactory.build { msg -> logs.add(msg) }

        // The controller may throw on response parsing for Unit type, but the request is still recorded
        runCatching {
            controller.send(
                TelegramMessage(
                    text = "Hello Telegram",
                    bots = listOf(bot),
                ),
            )
        }

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        val body = request.body.readUtf8()
        // Body contains chat_id (not modified by controller transformations)
        assertTrue(body.contains("-100123456789"), "Body should contain chat id")
        // The path should contain the bot token
        assertTrue(request.path?.contains("123456:FAKE_TOKEN") == true, "Path should contain bot token")
    }

    @Test
    fun `send uses TelegramMessage defaults`() {
        val message =
            TelegramMessage(
                text = "minimal",
                bots = emptyList(),
            )
        assertEquals("", message.header)
        assertEquals(emptyList<String>(), message.userMentions)
        assertEquals(emptyList<IssueSource>(), message.issueSources)
    }

    @Test
    fun `send links each issue source to its own host`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))

        val bot = makeBot()
        val controller = TelegramControllerFactory.build { msg -> logs.add(msg) }

        runCatching {
            controller.send(
                TelegramMessage(
                    text = "BASE-1 and LEG-2 shipped",
                    bots = listOf(bot),
                    issueSources =
                        listOf(
                            IssueSource(numberPattern = "BASE-\\d+", urlPrefix = "https://jira1/browse/"),
                            IssueSource(numberPattern = "LEG-\\d+", urlPrefix = "https://jira2/browse/"),
                        ),
                ),
            )
        }

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("https://jira1/browse/BASE-1"), "BASE key should link to its own host")
        assertTrue(body.contains("https://jira2/browse/LEG-2"), "LEG key should link to its own host")
    }

    @Test
    fun `send leaves keys unlinked when source has no url prefix`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))

        val bot = makeBot()
        val controller = TelegramControllerFactory.build { msg -> logs.add(msg) }

        runCatching {
            controller.send(
                TelegramMessage(
                    text = "APP-123 fixed",
                    bots = listOf(bot),
                    issueSources =
                        listOf(
                            IssueSource(numberPattern = "APP-\\d+", urlPrefix = null),
                        ),
                ),
            )
        }

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("APP-123"), "Issue key should still be present")
        assertTrue(!body.contains("<a href"), "No link markup should be produced without a url prefix")
    }
}
