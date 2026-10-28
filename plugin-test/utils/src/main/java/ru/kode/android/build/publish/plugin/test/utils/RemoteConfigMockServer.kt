package ru.kode.android.build.publish.plugin.test.utils

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.File
import java.security.KeyPairGenerator
import java.util.Base64

/**
 * Serves the OAuth token, `listVersions` and the template of the Firebase Remote Config project `demo`.
 * Version 1 has `shared_key` conditional value `false`, the active version 2 has `true`.
 */
fun MockWebServer.serveRemoteConfig() {
    dispatcher =
        object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path == "/token" -> json("""{"access_token":"test-token","expires_in":3600}""")
                    path.startsWith("/v1/projects/demo/remoteConfig:listVersions") ->
                        json(
                            """
                            {"versions":[{"versionNumber":"2","updateTime":"2026-10-01T00:00:00Z",
                              "updateUser":{"email":"dev@example.com"},"description":"enable shared"}]}
                            """.trimIndent(),
                        )
                    path == "/v1/projects/demo/remoteConfig?versionNumber=1" -> json(template(sharedValue = "false"))
                    path.startsWith("/v1/projects/demo/remoteConfig") -> json(template(sharedValue = "true"))
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
}

/**
 * Writes a service account JSON with a fresh RSA key whose `token_uri` points at this server.
 */
fun MockWebServer.writeServiceAccount(file: File) {
    val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    val pem = Base64.getMimeEncoder().encodeToString(keyPair.private.encoded).replace("\r\n", "\\n")
    file.parentFile.mkdirs()
    file.writeText(
        """
        {
          "client_email": "audit@demo.iam.gserviceaccount.com",
          "private_key": "-----BEGIN PRIVATE KEY-----\n$pem\n-----END PRIVATE KEY-----\n",
          "token_uri": "${url("/token")}"
        }
        """.trimIndent(),
    )
}

private fun template(sharedValue: String) =
    """
    {
      "conditions": [{"name": "new_app", "expression": "app.version.>=(['0.1'])"}],
      "parameters": {
        "shared_key": {
          "defaultValue": {"value": "false"},
          "conditionalValues": {"new_app": {"value": "$sharedValue"}}
        },
        "removed_key": {"defaultValue": {"value": "1"}},
        "stale_key": {"defaultValue": {"value": "1"}, "description": "old experiment"},
        "expired_key": {"defaultValue": {"value": "1"}, "description": "Old banner. owner=@ivan expires=2020-01-01"},
        "literal_key": {"defaultValue": {"value": "1"}},
        "survey_42": {"defaultValue": {"value": "1"}},
        "ios_only": {"defaultValue": {"value": "1"}}
      },
      "version": {"versionNumber": "2"}
    }
    """.trimIndent()

private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)
