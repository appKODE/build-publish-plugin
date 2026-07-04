package ru.kode.android.build.publish.plugin.nextcloud

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
import ru.kode.android.build.publish.plugin.nextcloud.controller.NextcloudController
import ru.kode.android.build.publish.plugin.nextcloud.controller.NextcloudControllerImpl
import ru.kode.android.build.publish.plugin.nextcloud.network.api.NextcloudApi
import java.io.File

class NextcloudControllerImplTest {
    private val server = MockWebServer()

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    private fun controller(): NextcloudController {
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }
        val api =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(NextcloudApi::class.java)
        return NextcloudControllerImpl(
            baseUrl = server.url("/").toString(),
            username = "user",
            api = api,
        )
    }

    private fun tempApk(): File {
        val file = File.createTempFile("nextcloud-upload", ".apk")
        file.writeText("apk-content")
        file.deleteOnExit()
        return file
    }

    @Test
    fun `resolveRemoteFilePath joins path and file name`() {
        val result = controller().resolveRemoteFilePath(remotePath = "builds", fileName = "app.apk")
        assertEquals("builds/app.apk", result)
    }

    @Test
    fun `resolveRemoteFilePath normalizes leading and trailing slashes`() {
        val result = controller().resolveRemoteFilePath(remotePath = "//builds/nightly//", fileName = "/app.apk")
        assertEquals("builds/nightly/app.apk", result)
    }

    @Test
    fun `uploadFile puts file to WebDAV endpoint using resolved dav user`() {
        // resolveDavUser -> GET ocs/v2.php/cloud/user
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"ocs":{"meta":{"status":"ok","statuscode":200},"data":{"id":"user"}}}"""),
        )
        // uploadFileByDav -> PUT remote.php/dav/files/{user}/...
        server.enqueue(MockResponse().setResponseCode(201))

        controller().uploadFile(
            remotePath = "builds",
            remoteFileName = "app.apk",
            file = tempApk(),
        )

        val userRequest = server.takeRequest()
        assertTrue(userRequest.path?.contains("cloud/user") == true, "First request should resolve current user")

        val uploadRequest = server.takeRequest()
        assertEquals("PUT", uploadRequest.method)
        assertEquals(
            "/remote.php/dav/files/user/builds/app.apk",
            uploadRequest.path,
        )
    }
}
