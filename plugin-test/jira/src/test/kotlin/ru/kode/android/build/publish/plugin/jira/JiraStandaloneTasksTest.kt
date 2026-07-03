package ru.kode.android.build.publish.plugin.jira

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File

class JiraStandaloneTasksTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `addJiraFixVersion task registered when plugin applied without Foundation or AGP`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.jira",
            pluginConfigBlock =
                """
                buildPublishJira {
                    auth {
                        common {
                            instance("default") {
                                baseUrl.set("https://jira.example.com")
                                credentials.username.set("user")
                                credentials.password.set("pass")
                            }
                        }
                    }
                    automation {
                        common {
                            projects {
                                project("app") {
                                    projectKey.set("APP")
                                }
                            }
                        }
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Jira.ADD_FIX_VERSION)
    }
}
