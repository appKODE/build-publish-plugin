package ru.kode.android.build.publish.plugin.firebase

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.firebase.controller.FirebaseRemoteConfigControllerFactory
import ru.kode.android.gradle.commons.logger.pluginLoggerFromLog
import java.io.File
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.Base64

class FirebaseRemoteConfigControllerTest {
    private val server = MockWebServer()
    private val requests = mutableListOf<RecordedRequest>()

    @TempDir
    lateinit var tempDir: File

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `should authorize with service account token and merge parameter groups`() {
        server.dispatcher = dispatcher()
        val controller = FirebaseRemoteConfigControllerFactory.build(credentials(), pluginLoggerFromLog {}, baseUrl())

        val template = controller.getTemplate("demo")
        controller.getTemplate("demo", versionNumber = "7")

        assertEquals(setOf("top_key", "grouped_key"), template.allParameters.keys)
        assertEquals("app.version >= '1.0'", template.conditions.single().expression)
        assertEquals("on", template.allParameters.getValue("grouped_key").conditionalValues.getValue("new").display())
        assertEquals("7", template.version?.versionNumber)
        val tokenRequests = requests.filter { it.path == "/token" }
        assertEquals(1, tokenRequests.size, "token must be cached between calls")
        val assertion = tokenRequests.single().body.readUtf8()
        assertTrue(assertion.contains("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer"))
        val apiRequests = requests.filter { it.path!!.startsWith("/v1/") }
        assertTrue(apiRequests.all { it.getHeader("Authorization") == "Bearer test-token" })
        assertEquals("/v1/projects/demo/remoteConfig?versionNumber=7", apiRequests.last().path)
    }

    @Test
    fun `should follow pagination when listing versions`() {
        server.dispatcher = dispatcher()
        val controller = FirebaseRemoteConfigControllerFactory.build(credentials(), pluginLoggerFromLog {}, baseUrl())

        val versions = controller.listVersions("demo", Instant.parse("2026-01-01T00:00:00Z"))

        assertEquals(listOf("9", "8"), versions.map { it.versionNumber })
        assertEquals("dev@example.com", versions.first().updateUser?.email)
        assertTrue(requests.any { it.path!!.contains("startTime=2026-01-01T00%3A00%3A00Z") })
    }

    private fun baseUrl() = server.url("/").toString()

    private fun credentials(): File {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val pem = Base64.getMimeEncoder().encodeToString(keyPair.private.encoded).replace("\r\n", "\\n")
        return File(tempDir, "sa.json").apply {
            writeText(
                """
                {
                  "type": "service_account",
                  "client_email": "audit@demo.iam.gserviceaccount.com",
                  "private_key": "-----BEGIN PRIVATE KEY-----\n$pem\n-----END PRIVATE KEY-----\n",
                  "token_uri": "${server.url("/token")}"
                }
                """.trimIndent(),
            )
        }
    }

    private fun dispatcher() =
        object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.path.orEmpty()
                val body =
                    when {
                        path == "/token" -> """{"access_token":"test-token","expires_in":3600}"""
                        path.contains(":listVersions") && path.contains("pageToken=next") ->
                            """{"versions":[{"versionNumber":"8"}]}"""
                        path.contains(":listVersions") ->
                            """{"versions":[{"versionNumber":"9","updateUser":{"email":"dev@example.com"}}],
                               "nextPageToken":"next"}"""
                        else -> TEMPLATE
                    }
                return MockResponse().setBody(body)
            }
        }
}

private const val TEMPLATE = """
{
  "conditions": [{"name": "new", "expression": "app.version >= '1.0'", "tagColor": "BLUE"}],
  "parameters": {"top_key": {"defaultValue": {"useInAppDefault": true}, "valueType": "BOOLEAN"}},
  "parameterGroups": {
    "features": {
      "parameters": {
        "grouped_key": {"defaultValue": {"value": "off"}, "conditionalValues": {"new": {"value": "on"}}}
      }
    }
  },
  "version": {"versionNumber": "7", "updateTime": "2026-01-02T00:00:00Z"}
}
"""
