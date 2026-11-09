package ru.kode.android.build.publish.plugin.yandexdisk

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.yandexdisk.controller.YandexDiskController
import ru.kode.android.build.publish.plugin.yandexdisk.controller.YandexDiskControllerImpl
import ru.kode.android.build.publish.plugin.yandexdisk.network.factory.YandexDiskApiFactory
import ru.kode.android.build.publish.plugin.yandexdisk.network.factory.YandexDiskClientFactory
import ru.kode.android.gradle.commons.logger.pluginLoggerFromLog
import java.io.File
import java.io.IOException
import java.net.InetAddress

class YandexDiskControllerImplTest {
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
    private fun controller(): YandexDiskController {
        val baseUrl = "http://localhost:${api.port}/"
        val client = YandexDiskClientFactory.build("secret-token", pluginLoggerFromLog {}, apiBaseUrl = baseUrl)
        return YandexDiskControllerImpl(YandexDiskApiFactory.build(client, baseUrl))
    }

    private fun tempApk(): File =
        File.createTempFile("yandex-upload", ".apk").apply {
            writeText("apk-content")
            deleteOnExit()
        }

    @Test
    fun `uploadFile creates folders, requests upload link and puts bytes without the token`() {
        api.enqueue(MockResponse().setResponseCode(201))
        api.enqueue(MockResponse().setResponseCode(409))
        api.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"href":"http://127.0.0.1:${uploadHost.port}/upload-target","method":"PUT","templated":false}""",
                ),
        )
        uploadHost.enqueue(MockResponse().setResponseCode(201))

        controller().uploadFile(remotePath = "/builds//nightly/", remoteFileName = "app.apk", file = tempApk())

        val folder1 = api.takeRequest()
        assertEquals("PUT", folder1.method)
        assertEquals("disk:/builds", folder1.requestUrl!!.queryParameter("path"))
        assertEquals("OAuth secret-token", folder1.getHeader("Authorization"))
        assertEquals("disk:/builds/nightly", api.takeRequest().requestUrl!!.queryParameter("path"))

        val link = api.takeRequest()
        assertEquals("GET", link.method)
        assertEquals("/v1/disk/resources/upload", link.requestUrl!!.encodedPath)
        assertEquals("disk:/builds/nightly/app.apk", link.requestUrl!!.queryParameter("path"))
        assertEquals("true", link.requestUrl!!.queryParameter("overwrite"))
        assertEquals("OAuth secret-token", link.getHeader("Authorization"))

        val upload = uploadHost.takeRequest()
        assertEquals("PUT", upload.method)
        assertEquals("/upload-target", upload.path)
        assertEquals("apk-content", upload.body.readUtf8())
        assertNull(upload.getHeader("Authorization"))
    }

    @Test
    fun `uploadFile treats 202 Accepted from upload host as success`() {
        api.enqueue(MockResponse().setResponseCode(201))
        api.enqueue(MockResponse().setResponseCode(200).setBody("""{"href":"http://127.0.0.1:${uploadHost.port}/u"}"""))
        uploadHost.enqueue(MockResponse().setResponseCode(202))

        controller().uploadFile(remotePath = "builds", remoteFileName = "app.apk", file = tempApk())

        assertEquals(1, uploadHost.requestCount)
    }

    @Test
    fun `uploadFile fails with status code when upload host rejects the file`() {
        api.enqueue(MockResponse().setResponseCode(201))
        api.enqueue(MockResponse().setResponseCode(200).setBody("""{"href":"http://127.0.0.1:${uploadHost.port}/u"}"""))
        uploadHost.enqueue(MockResponse().setResponseCode(507).setBody("Insufficient Storage"))

        val error =
            assertThrows(IOException::class.java) {
                controller().uploadFile(remotePath = "builds", remoteFileName = "app.apk", file = tempApk())
            }
        assertTrue(error.message!!.contains("507"))
    }

    @Test
    fun `uploadFile fails on unauthorized upload link request`() {
        api.enqueue(MockResponse().setResponseCode(201))
        api.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"UnauthorizedError"}"""))

        val error =
            assertThrows(IOException::class.java) {
                controller().uploadFile(remotePath = "builds", remoteFileName = "app.apk", file = tempApk())
            }
        assertTrue(error.message!!.contains("401"))
        assertEquals(0, uploadHost.requestCount)
    }

    @Test
    fun `publishFile publishes resource when it has no public link`() {
        api.enqueue(MockResponse().setResponseCode(200).setBody("""{}"""))
        api.enqueue(MockResponse().setResponseCode(200).setBody("""{"href":"x","method":"GET"}"""))
        api.enqueue(MockResponse().setResponseCode(200).setBody("""{"public_url":"https://yadi.sk/d/abc"}"""))

        val share = controller().publishFile(remotePath = "builds", remoteFileName = "app.apk")

        assertEquals("https://yadi.sk/d/abc", share.url)
        assertFalse(share.reused)
        val meta = api.takeRequest()
        assertEquals("public_url", meta.requestUrl!!.queryParameter("fields"))
        val publish = api.takeRequest()
        assertEquals("PUT", publish.method)
        assertEquals("/v1/disk/resources/publish", publish.requestUrl!!.encodedPath)
        assertEquals("disk:/builds/app.apk", publish.requestUrl!!.queryParameter("path"))
    }

    @Test
    fun `publishFile reuses an existing public link`() {
        api.enqueue(MockResponse().setResponseCode(200).setBody("""{"public_url":"https://yadi.sk/d/old"}"""))

        val share = controller().publishFile(remotePath = "builds", remoteFileName = "app.apk")

        assertEquals("https://yadi.sk/d/old", share.url)
        assertTrue(share.reused)
        assertEquals(1, api.requestCount)
    }

    @Test
    fun `resolveRemoteFilePath normalizes slashes`() {
        assertEquals("builds/nightly/app.apk", controller().resolveRemoteFilePath("//builds/nightly//", "/app.apk"))
    }
}
