package ru.kode.android.build.publish.plugin.sender

import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.serveRemoteConfig
import ru.kode.android.build.publish.plugin.test.utils.writeServiceAccount
import java.io.File

class SenderRemoteConfigTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File
    private val server = MockWebServer()

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
        server.serveRemoteConfig()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `remote config report is written without Android and configuration cache is supported`() {
        createProject()

        val result = projectDir.runTask(TASK, cliArguments = listOf("--info"))
        val ccResult = projectDir.runTask(TASK, cliArguments = listOf("--configuration-cache"))

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain("app.version=1.0.2, app.build=2 from tag v1.0.2")
        ccResult.outputShouldContain("BUILD SUCCESSFUL")
        val report = projectDir.getFile("build/firebase/remote-config/report.json").readText()
        listOf(
            """"currentTag": "v1.0.2"""",
            """"previousTag": "v1.0.1"""",
            """"type": "ADDED_IN_RELEASE"""",
            """"key": "new_key"""",
            """"type": "TARGETS_RELEASE"""",
            """"type": "POTENTIALLY_STALE"""",
        ).forEach { assertTrue(report.contains(it), "report.json must contain $it, was:\n$report") }
        val sarif = projectDir.getFile("build/firebase/remote-config/report.sarif").readText()
        listOf(
            """"ruleId": "AddedInRelease"""",
            """"uri": "src/main/kotlin/RemoteKeys.kt"""",
        ).forEach { assertTrue(sarif.contains(it), "report.sarif must contain $it, was:\n$sarif") }
    }

    private fun createProject() {
        server.writeServiceAccount(projectDir.getFile("sa.json"))
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.sender",
            pluginConfigBlock =
                """
                buildPublishSender {
                    firebaseRemoteConfig {
                        serviceCredentialsFile = file("sa.json")
                        projectId.set("demo")
                        apiBaseUrl.set("${server.url("/")}")
                        buildTagPattern.set('v\\d+\\.\\d+\\.(\\d+)')
                        keySource("keys") {
                            include("src/main/**/RemoteKeys.kt")
                            extractor.set(regex('const val \\w+_KEY = "([^"]+)"'))
                        }
                        usageSearch("src/main/**")
                    }
                }
                """.trimIndent(),
        )
        val keysFile = projectDir.getFile("src/main/kotlin/RemoteKeys.kt")
        val git = projectDir.initGit()
        keysFile.writeText("""const val SHARED_KEY = "shared_key"""")
        git.addAllAndCommit("Initial commit")
        git.tag.addNamed("v1.0.1")
        keysFile.writeText("const val SHARED_KEY = \"shared_key\"\nconst val NEW_KEY = \"new_key\"")
        git.addAllAndCommit("Add new key")
        git.tag.addNamed("v1.0.2")
    }

    private companion object {
        const val TASK = "checkFirebaseRemoteConfig"
    }
}
