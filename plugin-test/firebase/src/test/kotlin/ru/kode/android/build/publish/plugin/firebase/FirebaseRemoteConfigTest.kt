package ru.kode.android.build.publish.plugin.firebase

import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FirebaseConfig
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import ru.kode.android.build.publish.plugin.test.utils.serveRemoteConfig
import ru.kode.android.build.publish.plugin.test.utils.writeServiceAccount
import java.io.File

class FirebaseRemoteConfigTest {
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
    fun `remote config report is written and configuration cache is supported`() {
        createProject(failOnMissing = false)

        val result = projectDir.runTask(TASK)
        val ccResult = projectDir.runTask(TASK, cliArguments = listOf("--configuration-cache"))
        val ccReuseResult = projectDir.runTask(TASK, cliArguments = listOf("--configuration-cache"))

        result.outputShouldContain("BUILD SUCCESSFUL")
        ccResult.outputShouldContain("BUILD SUCCESSFUL")
        ccReuseResult.outputShouldContain("Reusing configuration cache.")
        val report = projectDir.getFile("app/build/firebase/remote-config/debug/report.json").readText()
        listOf(
            """"currentTag": "v1.0.2-debug"""",
            """"previousTag": "v1.0.1-debug"""",
            """"type": "ADDED_IN_RELEASE"""",
            """"type": "REMOVED_IN_RELEASE"""",
            """"type": "MISSING_IN_REMOTE"""",
            """"key": "new_key"""",
            """"key": "removed_key"""",
            """"type": "UNUSED_IN_REMOTE"""",
            """"key": "stale_key"""",
            """"type": "TARGETS_RELEASE"""",
            """"condition": "new_app"""",
            """"type": "CHANGED_SINCE_RELEASE"""",
            """"updateUser": "dev@example.com"""",
            """"type": "POTENTIALLY_STALE"""",
            """"key": "expired_key"""",
            """"reason": "expired on 2020-01-01"""",
            """"type": "DEFAULTS_MISMATCH"""",
            """"inAppValue": "true"""",
        ).forEach { assertTrue(report.contains(it), "report.json must contain $it, was:\n$report") }
        listOf("literal_key", "survey_42", "ios_only").forEach {
            assertTrue(!report.contains("\"$it\""), "$it must not be reported, was:\n$report")
        }
        assertTrue(projectDir.getFile("app/build/firebase/remote-config/debug/report.md").readText().contains("`new_key`"))
        val sarif = projectDir.getFile("app/build/firebase/remote-config/debug/report.sarif").readText()
        listOf(
            """"version": "2.1.0"""",
            """"ruleId": "MissingInRemote"""",
            """"ruleId": "DefaultsMismatch"""",
            """"uri": "app/src/main/java/RemoteKeys.kt"""",
            """"uri": "app/src/main/res/xml/remote_config_defaults.xml"""",
        ).forEach { assertTrue(sarif.contains(it), "report.sarif must contain $it, was:\n$sarif") }
    }

    @Test
    fun `remote config check fails when finding has Fail severity`() {
        createProject(failOnMissing = true)

        val result = projectDir.runTaskWithFail(TASK)

        result.outputShouldContain("MissingInRemote")
        assertTrue(projectDir.getFile("app/build/firebase/remote-config/debug/report.json").exists())
    }

    private fun createProject(failOnMissing: Boolean) {
        server.writeServiceAccount(projectDir.getFile("app/sa.json"))
        val severity = if (failOnMissing) """severity("MissingInRemote", "Fail")""" else ""
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig = FoundationConfig(output = FoundationConfig.Output(baseFileName = "autotest")),
            firebaseConfig =
                FirebaseConfig(
                    remoteConfigCommon =
                        """
                        serviceCredentialsFile = project.file("sa.json")
                        projectId.set("demo")
                        apiBaseUrl.set("${server.url("/")}")
                        keySource("keys") {
                            include("app/src/main/**/RemoteKeys.kt")
                            extractor.set(regex('const val \\w+_KEY = "([^"]+)"'))
                        }
                        usageSearch("app/src/main/**")
                        inAppDefaults("app/src/main/res/xml/remote_config_defaults.xml")
                        dynamicKeyPrefixes.add("survey_")
                        ignoreKeys.add("ios_.*")
                        $severity
                        """.trimIndent(),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
            configureApplicationVariants = true,
        )
        val keysFile = projectDir.getFile("app/src/main/java/RemoteKeys.kt")
        val git = projectDir.initGit()
        keysFile.writeText(keys("SHARED_KEY" to "shared_key", "REMOVED_KEY" to "removed_key"))
        projectDir.getFile("app/src/main/java/Other.kt").writeText("""val literal = "literal_key"""")
        git.addAllAndCommit("Initial commit")
        git.tag.addNamed("v1.0.1-debug")
        keysFile.writeText(keys("SHARED_KEY" to "shared_key", "NEW_KEY" to "new_key"))
        projectDir.getFile("app/src/main/res/xml/remote_config_defaults.xml").writeText(IN_APP_DEFAULTS)
        git.addAllAndCommit("Add new key")
        git.tag.addNamed("v1.0.2-debug")
    }

    private fun keys(vararg keys: Pair<String, String>) = keys.joinToString("\n") { (name, value) -> """const val $name = "$value"""" }

    private companion object {
        const val TASK = "checkFirebaseRemoteConfigDebug"
        val IN_APP_DEFAULTS =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <defaultsMap>
                <entry>
                    <key>shared_key</key>
                    <value>true</value>
                </entry>
            </defaultsMap>
            """.trimIndent()
    }
}
