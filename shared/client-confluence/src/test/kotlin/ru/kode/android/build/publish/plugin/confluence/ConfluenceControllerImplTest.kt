package ru.kode.android.build.publish.plugin.confluence

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import ru.kode.android.build.publish.plugin.confluence.controller.ConfluenceController
import ru.kode.android.build.publish.plugin.confluence.controller.ConfluenceControllerImpl
import ru.kode.android.build.publish.plugin.confluence.network.api.ConfluenceApi
import java.io.File

class ConfluenceControllerImplTest {
    private val server = MockWebServer()

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    private fun controller(): ConfluenceController {
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }
        val api =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(ConfluenceApi::class.java)
        return ConfluenceControllerImpl(
            baseUrl = server.url("/").toString().trimEnd('/'),
            api = api,
        )
    }

    private fun tempApk(): File {
        val file = File.createTempFile("confluence-upload", ".apk")
        file.writeText("apk-content")
        file.deleteOnExit()
        return file
    }

    @Test
    fun `uploadFile posts multipart attachment to page`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val file = tempApk()
        controller().uploadFile(pageId = "12345", file = file)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/rest/api/content/12345/child/attachment", request.path)
        assertTrue(
            request.getHeader("Content-Type")?.startsWith("multipart/form-data") == true,
            "Attachment upload should use multipart form data",
        )
        assertTrue(request.body.readUtf8().contains(file.name), "Body should contain the uploaded file name")
    }

    @Test
    fun `addComment posts comment with download link`() {
        server.enqueue(MockResponse().setResponseCode(200))

        controller().addComment(pageId = "12345", fileName = "app.apk")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/rest/api/content", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("download/attachments/12345/app.apk"), "Body should contain the download link")
        assertTrue(body.contains("app.apk"), "Body should contain the file name")
    }

    @Test
    fun `getAttachments returns mapped attachments`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"results":[{"id":"att1","title":"app.apk"}]}"""),
        )

        val attachments = controller().getAttachments(pageId = "12345")

        assertEquals(1, attachments.size)
        assertEquals("att1", attachments.first().id)
        assertEquals("app.apk", attachments.first().fileName)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(
            request.path?.startsWith("/rest/api/content/12345/child/attachment") == true,
            "Should request attachments for the page",
        )
    }

    @Test
    fun `removeAttachment deletes content by id`() {
        server.enqueue(MockResponse().setResponseCode(204))

        controller().removeAttachment(attachmentId = "att1")

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/rest/api/content/att1", request.path)
    }
}
