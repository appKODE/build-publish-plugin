package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.foundation.utils.BuildType
import ru.kode.android.build.publish.plugin.foundation.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.foundation.utils.ManifestProperties
import ru.kode.android.build.publish.plugin.foundation.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.foundation.utils.addNamed
import ru.kode.android.build.publish.plugin.foundation.utils.addNamedWithMessage
import ru.kode.android.build.publish.plugin.foundation.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.foundation.utils.currentDate
import ru.kode.android.build.publish.plugin.foundation.utils.extractManifestProperties
import ru.kode.android.build.publish.plugin.foundation.utils.find
import ru.kode.android.build.publish.plugin.foundation.utils.getFile
import ru.kode.android.build.publish.plugin.foundation.utils.initGit
import ru.kode.android.build.publish.plugin.foundation.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.foundation.utils.runTask
import ru.kode.android.build.publish.plugin.foundation.utils.runTasks
import java.io.File
import java.io.IOException

class FoundationChangelogTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble if all commits exists and not formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName = "v1.0.1-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "(TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.1-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "v1.0.1-debug",
            )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            No changes compared to the previous build
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble if all commits exists and not formed using message key with one described tag`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName = "v1.0.1-debug"
        val givenTagMessage = "First tag and build"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "(TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamedWithMessage(givenTagName, givenTagMessage)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.1-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = givenTagMessage,
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "v1.0.1-debug",
            )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            *First tag and build*
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble if all commits exists and formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName = "v1.0.1-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.1-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "v1.0.1-debug",
            )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • [auth flow]: wrap chat flow component in remember for prevent recomposition
            • Update pager behaviour
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble if all commits exists and formed using message key with one described tag`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName = "v1.0.1-debug"
        val givenTagMessage = "First release build"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamedWithMessage(givenTagName, givenTagMessage)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.1-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = givenTagMessage,
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "v1.0.1-debug",
            )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            *First release build*
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • [auth flow]: wrap chat flow component in remember for prevent recomposition
            • Update pager behaviour
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble simultaneously if all commits exists and formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName = "v1.0.1-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTasks(givenAssembleTask, givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.1-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "v1.0.1-debug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            result.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertEquals(
            result.output
                .split("\n")
                .filter {
                    it.contains("Task :app:getLastTagDebug") ||
                        it.contains("Task :app:generateChangelogDebug")
                }
                .size,
            2,
            "Each task executed without duplications",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • [auth flow]: wrap chat flow component in remember for prevent recomposition
            • Update pager behaviour
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    // TODO: Should we trigger assemble automatically before getLastTag?
    @Test
    @Throws(IOException::class)
    fun `generate changelog without assemble if all commits exists and formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName = "v1.0.1-debug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.1-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()

        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • [auth flow]: wrap chat flow component in remember for prevent recomposition
            • Update pager behaviour
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble if all commits exists and partially formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName = "v1.0.1-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.1-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "v1.0.1-debug",
            )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • Update pager behaviour
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble simultaneously if all commits exists and partially formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName = "v1.0.1-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTasks(givenAssembleTask, givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.1-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "v1.0.1-debug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            result.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertEquals(
            result.output
                .split("\n")
                .filter {
                    it.contains("Task :app:getLastTagDebug") ||
                        it.contains("Task :app:generateChangelogDebug")
                }
                .size,
            2,
            "Each task executed without duplications",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • Update pager behaviour
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    // TODO: Should we trigger assemble automatically before getLastTag?
    @Test
    @Throws(IOException::class)
    fun `generate changelog without assemble if all commits exists and partially formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName = "v1.0.1-debug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.1-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()

        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • Update pager behaviour
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble if all commits exists and formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "CHANGELOG: [profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName2).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.2-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "2",
                versionName = "v1.0.2-debug",
            )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [android] Add logic to open accounts
            • [profile flow]: Add Compose screen with VM logic
            • Add readme file
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble if all commits exists and formed using message key with 2 described tags`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenTagMessage2 = "Add second build"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "CHANGELOG: [profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamedWithMessage(givenTagName2, givenTagMessage2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName2).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.2-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = givenTagMessage2,
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "2",
                versionName = "v1.0.2-debug",
            )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            *Add second build*
            • (TICKET-1815) [android] Add logic to open accounts
            • [profile flow]: Add Compose screen with VM logic
            • Add readme file
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble if all commits exists and not formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "(TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "[profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "(TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName2).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.2-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "2",
                versionName = "v1.0.2-debug",
            )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            No changes compared to the previous build (v1.0.1-debug)
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble simultaneously if all commits exists and formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "CHANGELOG: [profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)

        val result: BuildResult = projectDir.runTasks(givenAssembleTask, givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName2).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.2-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "2",
                versionName = "v1.0.2-debug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            result.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertEquals(
            result.output
                .split("\n")
                .filter {
                    it.contains("Task :app:getLastTagDebug") ||
                        it.contains("Task :app:generateChangelogDebug")
                }
                .size,
            2,
            "Each task executed without duplications",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [android] Add logic to open accounts
            • [profile flow]: Add Compose screen with VM logic
            • Add readme file
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    // TODO: Should we trigger assemble automatically before getLastTag?
    @Test
    @Throws(IOException::class)
    fun `generate changelog without assemble if all commits exists and formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "CHANGELOG: [profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.find(givenTagName2).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.2-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()

        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [android] Add logic to open accounts
            • [profile flow]: Add Compose screen with VM logic
            • Add readme file
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble if all commits exists and partially formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "[profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName2).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.2-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "2",
                versionName = "v1.0.2-debug",
            )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [android] Add logic to open accounts
            • Add readme file
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble simultaneously if all commits exists and partially formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "[profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)

        val result: BuildResult = projectDir.runTasks(givenAssembleTask, givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName2).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.2-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "2",
                versionName = "v1.0.2-debug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            result.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertEquals(
            result.output
                .split("\n")
                .filter {
                    it.contains("Task :app:getLastTagDebug") ||
                        it.contains("Task :app:generateChangelogDebug")
                }
                .size,
            2,
            "Each task executed without duplications",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [android] Add logic to open accounts
            • Add readme file
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    // TODO: Should we trigger assemble automatically before getLastTag?
    @Test
    @Throws(IOException::class)
    fun `generate changelog without assemble if all commits exists and partially formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "[profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.find(givenTagName2).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.2-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()

        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [android] Add logic to open accounts
            • Add readme file
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    // TODO: Should we trigger assemble automatically before getLastTag?
    @Test
    @Throws(IOException::class)
    fun `generate changelog without assemble if all commits exists and partially formed using message key with 3 tags, some of them on the same commit`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenTagName3 = "v1.0.3-debug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "[profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)
        git.tag.addNamed(givenTagName3)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.find(givenTagName2).id
        val expectedBuildNumber = "3"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.3-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()

        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [android] Add logic to open accounts
            • Add readme file
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    // TODO: Should we trigger assemble automatically before getLastTag?
    @Test
    @Throws(IOException::class)
    fun `generate changelog without assemble if all commits exists and partially formed using message key with 3 described tags, all of them on the some commit`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagMessage1 = "First build"
        val givenTagName2 = "v1.0.2-debug"
        val givenTagMessage2 = "Second build"
        val givenTagName3 = "v1.0.3-debug"
        val givenTagMessage3 = "Third build"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "[profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamedWithMessage(givenTagName1, givenTagMessage1)
        git.tag.addNamedWithMessage(givenTagName2, givenTagMessage2)
        git.tag.addNamedWithMessage(givenTagName3, givenTagMessage3)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.find(givenTagName2).id
        val expectedBuildNumber = "3"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.3-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = givenTagMessage3,
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()

        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            *Third build*
            • (TICKET-1815) [android] Add logic to open accounts
            • Add readme file
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • Update pager behaviour
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    // TODO: Should we trigger assemble automatically before getLastTag?
    @Test
    @Throws(IOException::class)
    fun `generate changelog without assemble if all commits exists and partially formed using message key with 3 tags, all of them on the some commit`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenTagName3 = "v1.0.3-debug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "[profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName1)
        git.tag.addNamed(givenTagName2)
        git.tag.addNamed(givenTagName3)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.find(givenTagName2).id
        val expectedBuildNumber = "3"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.3-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()

        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            • (TICKET-1815) [android] Add logic to open accounts
            • Add readme file
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • Update pager behaviour
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    // TODO: Should we trigger assemble automatically before getLastTag?
    @Test
    @Throws(IOException::class)
    fun `generate changelog without assemble if all commits exists and partially formed using message key with 3 described tags, some of them on the same commit`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "TICKET-\\\\d+",
                            issueUrlPrefix = "https://jira.example.com/browse/",
                            commitMessageKey = "CHANGELOG",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagMessage1 = "First build"
        val givenTagName2 = "v1.0.2-debug"
        val givenTagMessage2 = "Second build"
        val givenTagName3 = "v1.0.3-debug"
        val givenTagMessage3 = "Third build"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamedWithMessage(givenTagName1, givenTagMessage1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "[profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamedWithMessage(givenTagName2, givenTagMessage2)
        git.tag.addNamedWithMessage(givenTagName3, givenTagMessage3)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.find(givenTagName2).id
        val expectedBuildNumber = "3"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.3-debug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = givenTagMessage3,
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()

        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )

        val expectedChangelogFile =
            """
            *Third build*
            • (TICKET-1815) [android] Add logic to open accounts
            • Add readme file
            """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }
}
