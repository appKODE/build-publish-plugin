package ru.kode.android.build.publish.plugin.vk

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.vk.controller.VkController
import ru.kode.android.build.publish.plugin.vk.controller.VkControllerImpl
import ru.kode.android.build.publish.plugin.vk.network.factory.VkApiFactory
import ru.kode.android.build.publish.plugin.vk.network.factory.VkClientFactory
import ru.kode.android.gradle.commons.logger.pluginLoggerFromLog
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.URLDecoder

class VkControllerImplTest {
    private val api = MockWebServer()
    private val uploadHost = MockWebServer()

    @BeforeEach
    fun start() {
        api.start(InetAddress.getByName("127.0.0.1"), 0)
        uploadHost.start(InetAddress.getByName("127.0.0.1"), 0)
    }

    @AfterEach
    fun stop() {
        api.shutdown()
        uploadHost.shutdown()
    }

    // The API is reached as "localhost" and the upload URL as "127.0.0.1", so they are different hosts.
    private fun controller(): VkController {
        val baseUrl = "http://localhost:${api.port}/method/"
        val client = VkClientFactory.build("secret-token", pluginLoggerFromLog {}, apiBaseUrl = baseUrl)
        return VkControllerImpl(VkApiFactory.build(client, baseUrl))
    }

    private fun tempApk(): File =
        File.createTempFile("vk-upload", ".apk").apply {
            writeText("apk-content")
            deleteOnExit()
        }

    private fun form(body: String): Map<String, String> =
        body.split('&').associate {
            val (key, value) = it.split('=', limit = 2)
            key to URLDecoder.decode(value, "UTF-8")
        }

    @Test
    fun `sendDocument uploads document and sends it to the peer`() {
        api.enqueue(
            MockResponse().setBody(
                """{"response":{"upload_url":"http://127.0.0.1:${uploadHost.port}/upload.php?sig=x"}}""",
            ),
        )
        uploadHost.enqueue(MockResponse().setBody("""{"file":"uploaded-file-token"}"""))
        api.enqueue(
            MockResponse().setBody(
                """{"response":{"type":"doc","doc":""" +
                    """{"id":42,"owner_id":-100,"url":"https://vk.com/doc-100_42","access_key":"k1"}}}""",
            ),
        )
        api.enqueue(MockResponse().setBody("""{"response":777}"""))

        val result = controller().sendDocument(2000000001, tempApk(), "app-1.0-debug.apk", "New build")

        assertEquals(777, result.messageId)
        assertEquals("https://vk.com/doc-100_42", result.documentUrl)

        val server = api.takeRequest()
        assertEquals("/method/docs.getMessagesUploadServer", server.requestUrl!!.encodedPath)
        assertEquals("5.199", server.requestUrl!!.queryParameter("v"))
        assertEquals("Bearer secret-token", server.getHeader("Authorization"))
        assertEquals(mapOf("peer_id" to "2000000001", "type" to "doc"), form(server.body.readUtf8()))

        val upload = uploadHost.takeRequest()
        assertNull(upload.getHeader("Authorization"))
        assertEquals("/upload.php?sig=x", upload.path)
        val uploadBody = upload.body.readUtf8()
        assertTrue(uploadBody.contains("""name="file"; filename="app-1.0-debug.apk""""))
        assertTrue(uploadBody.contains("apk-content"))

        val save = api.takeRequest()
        assertEquals("/method/docs.save", save.requestUrl!!.encodedPath)
        assertEquals(mapOf("file" to "uploaded-file-token", "title" to "app-1.0-debug.apk"), form(save.body.readUtf8()))

        val send = api.takeRequest()
        assertEquals("/method/messages.send", send.requestUrl!!.encodedPath)
        val sendForm = form(send.body.readUtf8())
        assertEquals("2000000001", sendForm["peer_id"])
        assertEquals("doc-100_42_k1", sendForm["attachment"])
        assertEquals("New build", sendForm["message"])
        assertTrue(sendForm.containsKey("random_id"))
    }

    @Test
    fun `sendDocument omits access key and message when absent`() {
        api.enqueue(MockResponse().setBody("""{"response":{"upload_url":"http://127.0.0.1:${uploadHost.port}/u"}}"""))
        uploadHost.enqueue(MockResponse().setBody("""{"file":"f"}"""))
        api.enqueue(MockResponse().setBody("""{"response":{"type":"doc","doc":{"id":1,"owner_id":-5}}}"""))
        api.enqueue(MockResponse().setBody("""{"response":1}"""))

        controller().sendDocument(-5, tempApk(), "app.apk", null)

        repeat(2) { api.takeRequest() }
        val sendForm = form(api.takeRequest().body.readUtf8())
        assertEquals("doc-5_1", sendForm["attachment"])
        assertTrue(!sendForm.containsKey("message"))
    }

    @Test
    fun `sendDocument fails with VK error returned in HTTP 200`() {
        api.enqueue(MockResponse().setBody("""{"error":{"error_code":5,"error_msg":"User authorization failed"}}"""))

        val error =
            assertThrows(IOException::class.java) {
                controller().sendDocument(1, tempApk(), "app.apk", null)
            }
        assertTrue(error.message!!.contains("docs.getMessagesUploadServer"))
        assertTrue(error.message!!.contains("User authorization failed"))
        assertEquals(0, uploadHost.requestCount)
    }

    @Test
    fun `sendDocument fails when upload server rejects the file`() {
        api.enqueue(MockResponse().setBody("""{"response":{"upload_url":"http://127.0.0.1:${uploadHost.port}/u"}}"""))
        uploadHost.enqueue(MockResponse().setBody("""{"error":"invalid file","error_descr":"forbidden extension"}"""))

        val error =
            assertThrows(IOException::class.java) {
                controller().sendDocument(1, tempApk(), "app.apk", null)
            }
        assertTrue(error.message!!.contains("forbidden extension"))
        assertEquals(1, api.requestCount)
    }
}
