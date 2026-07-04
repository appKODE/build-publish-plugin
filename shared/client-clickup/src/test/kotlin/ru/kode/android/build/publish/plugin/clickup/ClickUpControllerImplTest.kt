package ru.kode.android.build.publish.plugin.clickup

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
import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpController
import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpControllerImpl
import ru.kode.android.build.publish.plugin.clickup.network.api.ClickUpApi
import ru.kode.android.build.publish.plugin.core.logger.pluginLoggerFromLog

class ClickUpControllerImplTest {
    private val server = MockWebServer()

    private val logger = pluginLoggerFromLog { }

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    private fun controller(): ClickUpController {
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }
        val api =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(ClickUpApi::class.java)
        return ClickUpControllerImpl(api = api, logger = logger)
    }

    @Test
    fun `addTagToTask posts to tag endpoint`() {
        server.enqueue(MockResponse().setResponseCode(200))

        controller().addTagToTask(taskId = "task1", tagName = "release")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v2/task/task1/tag/release", request.path)
    }

    @Test
    fun `addFieldToTask posts value in body`() {
        server.enqueue(MockResponse().setResponseCode(200))

        controller().addFieldToTask(taskId = "task1", fieldId = "field1", fieldValue = "1.0.0")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v2/task/task1/field/field1", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("1.0.0"), "Body should contain the field value")
    }

    @Test
    fun `getTaskName returns task name from task endpoint`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":"task1","name":"Update Slack publishing","tags":[],"custom_fields":[]}"""),
        )

        val name = controller().getTaskName("task1")

        assertEquals("Update Slack publishing", name)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.startsWith("/v2/task/task1") == true, "Should call the task endpoint")
    }

    @Test
    fun `getTaskName returns null when request fails`() {
        server.enqueue(MockResponse().setResponseCode(404))

        val name = controller().getTaskName("missing")

        assertEquals(null, name)
    }

    @Test
    fun `getOrCreateCustomFieldId returns existing field id`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"teams":[{"id":"t1","name":"ws"}]}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"spaces":[{"id":"s1","name":"space"}]}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"lists":[{"id":"l1"}]}"""))
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"fields":[{"id":"cf1","name":"Fix Version","type":"text"}]}"""),
        )

        val fieldId = controller().getOrCreateCustomFieldId(workspaceName = "ws", fieldName = "Fix Version")

        assertEquals("cf1", fieldId)
        assertEquals("/v2/team", server.takeRequest().path)
        assertEquals("/v2/team/t1/space", server.takeRequest().path)
        assertEquals("/v2/space/s1/list", server.takeRequest().path)
        assertEquals("/v2/list/l1/field", server.takeRequest().path)
    }

    @Test
    fun `getOrCreateCustomFieldId creates field when it does not exist`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"teams":[{"id":"t1","name":"ws"}]}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"spaces":[{"id":"s1","name":"space"}]}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"lists":[{"id":"l1"}]}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"fields":[]}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"field":{"id":"cf-new"}}"""))

        val fieldId = controller().getOrCreateCustomFieldId(workspaceName = "ws", fieldName = "Fix Version")

        assertEquals("cf-new", fieldId)
        repeat(4) { server.takeRequest() }
        val createRequest = server.takeRequest()
        assertEquals("POST", createRequest.method)
        assertEquals("/v2/list/l1/field", createRequest.path)
        assertTrue(createRequest.body.readUtf8().contains("Fix Version"), "Create body should contain field name")
    }
}
