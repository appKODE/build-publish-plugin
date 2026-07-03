package ru.kode.android.build.publish.plugin.jira

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.JiraConfig
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import java.io.File

/**
 * Offline (no Jira network) test validating configuration and task registration for the
 * multi-project / multi-instance Jira DSL: the `projects { }` container, multiple `auth`
 * configurations, and per-project `instanceName`.
 *
 * Evaluating this DSL instantiates the managed `projects` container and registers one Jira build
 * service per auth configuration, so a successful `tasks` run proves the multi-instance wiring holds
 * together without touching a real Jira instance.
 *
 * Config-time validation (duplicate project key / unknown auth name) only runs while registering the
 * per-variant automation task, which needs the Foundation plugin and Android build variants. The two
 * `configuration fails …` cases therefore use [createAndroidProject] (Foundation + Jira) and expect a
 * `buildAndFail` — the validation throws during configuration, before any Jira network call.
 */
class JiraMultiProjectRegistrationTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `standalone tasks registered with multiple auth configs and projects block`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.jira",
            pluginConfigBlock =
                """
                buildPublishJira {
                    auth {
                        common {
                            instance("default") {
                                baseUrl.set("https://jira1.example.com")
                                credentials.username.set("user1")
                                credentials.password.set("pass1")
                            }
                            instance("secondary") {
                                baseUrl.set("https://jira2.example.com")
                                credentials.username.set("user2")
                                credentials.password.set("pass2")
                            }
                        }
                    }
                    automation {
                        common {
                            targetStatusName.set("Ready for QA")
                            projects {
                                project("main") {
                                    projectKey.set("APP")
                                }
                                project("legacy") {
                                    projectKey.set("LEG")
                                    instanceName.set("secondary")
                                    targetStatusName.set("Done")
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
        result.outputShouldContain(TaskNames.Jira.TRANSITION_ISSUE)
    }

    @Test
    fun `single project shorthand registers without a projects block`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.jira",
            pluginConfigBlock =
                """
                buildPublishJira {
                    auth {
                        common {
                            instance("default") {
                                baseUrl.set("https://jira1.example.com")
                                credentials.username.set("user1")
                                credentials.password.set("pass1")
                            }
                        }
                    }
                    automation {
                        common {
                            // Shorthand: single project, no surrounding projects { } block
                            project("main") {
                                projectKey.set("APP")
                                targetStatusName.set("Ready for QA")
                            }
                        }
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Jira.ADD_FIX_VERSION)
        result.outputShouldContain(TaskNames.Jira.TRANSITION_ISSUE)
    }

    @Test
    fun `unnamed single project shorthand registers without a projects block`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.jira",
            pluginConfigBlock =
                """
                buildPublishJira {
                    auth {
                        common {
                            instance("default") {
                                baseUrl.set("https://jira1.example.com")
                                credentials.username.set("user1")
                                credentials.password.set("pass1")
                            }
                        }
                    }
                    automation {
                        common {
                            // Shorthand: single unnamed project, no name and no projects { } block
                            project {
                                projectKey.set("APP")
                                targetStatusName.set("Ready for QA")
                            }
                        }
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Jira.ADD_FIX_VERSION)
        result.outputShouldContain(TaskNames.Jira.TRANSITION_ISSUE)
    }

    @Test
    fun `configuration fails when two projects declare the same project key`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "APP-\\\\d+",
                            issueUrlPrefix = "https://jira1.example.com/browse/",
                        ),
                ),
            jiraConfig =
                JiraConfig(
                    auth =
                        JiraConfig.Auth(
                            baseUrl = "https://jira1.example.com",
                            username = "user1",
                            password = "pass1",
                        ),
                    automation =
                        JiraConfig.Automation(
                            projects =
                                listOf(
                                    JiraConfig.Project(
                                        name = "first",
                                        projectKey = "APP",
                                        targetStatusName = "Ready for QA",
                                    ),
                                    JiraConfig.Project(
                                        name = "second",
                                        projectKey = "APP",
                                        targetStatusName = "Ready for QA",
                                    ),
                                ),
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val result = projectDir.runTaskWithFail("tasks")

        result.outputShouldContain("DUPLICATE JIRA PROJECT KEY")
        result.outputShouldContain("APP")
    }

    @Test
    fun `configuration fails when a project references an unknown instanceName`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "APP-\\\\d+",
                            issueUrlPrefix = "https://jira1.example.com/browse/",
                        ),
                ),
            jiraConfig =
                JiraConfig(
                    auth =
                        JiraConfig.Auth(
                            baseUrl = "https://jira1.example.com",
                            username = "user1",
                            password = "pass1",
                        ),
                    automation =
                        JiraConfig.Automation(
                            projects =
                                listOf(
                                    JiraConfig.Project(
                                        name = "main",
                                        projectKey = "APP",
                                        instanceName = "doesNotExist",
                                        targetStatusName = "Ready for QA",
                                    ),
                                ),
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val result = projectDir.runTaskWithFail("tasks")

        result.outputShouldContain("UNKNOWN JIRA AUTH INSTANCE")
        result.outputShouldContain("doesNotExist")
    }
}
