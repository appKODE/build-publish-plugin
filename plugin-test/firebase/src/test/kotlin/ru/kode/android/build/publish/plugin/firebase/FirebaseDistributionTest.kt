package ru.kode.android.build.publish.plugin.firebase

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.DefaultConfig
import ru.kode.android.build.publish.plugin.test.utils.FirebaseConfig
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import java.io.File
import java.io.IOException

class FirebaseDistributionTest {

    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `firebase build distribution available with full distribution config`() {
        val googleServicesFile = projectDir.getFile("app/google-services.json")
        File(System.getProperty("FIREBASE_GOOGLE_SERVICES_FILE_PATH"))
            .copyTo(googleServicesFile, true)
        val serviceCredentialsFile = projectDir.getFile("app/service-credentials.json")
        File(System.getProperty("FIREBASE_SERVICE_CREDENTIALS_FILE_PATH"))
            .copyTo(serviceCredentialsFile, true)

        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            firebaseConfig = FirebaseConfig(
                distributionCommon = FirebaseConfig.Distribution(
                    serviceCredentialsFilePath = serviceCredentialsFile.name,
                    appId = System.getProperty("FIREBASE_APP_ID"),
                    testerGroups = listOf("testers"),
                    artifactType = "ArtifactType.Apk",
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent(),
            import = "import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType",
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val givenAppDistributionTask = "appDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val generateChangelogResult: BuildResult = projectDir.runTask(givenChangelogTask)
        val distributionResult: BuildResult = projectDir.runTask(givenAppDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            generateChangelogResult.output.contains("BUILD SUCCESSFUL"),
            "Generate changelog successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Firebase distribution successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `firebase build distribution available with full distribution config for internal`() {
        val googleServicesFile = projectDir.getFile("app/google-services.json")
        File(System.getProperty("FIREBASE_GOOGLE_SERVICES_FILE_PATH"))
            .copyTo(googleServicesFile, true)
        val serviceCredentialsFile = projectDir.getFile("app/service-credentials.json")
        File(System.getProperty("FIREBASE_SERVICE_CREDENTIALS_FILE_PATH"))
            .copyTo(serviceCredentialsFile, true)

        projectDir.createAndroidProject(
            defaultConfig = DefaultConfig(
                applicationId = "com.example.build.types.android.internal",
            ),
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("internal")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            firebaseConfig = FirebaseConfig(
                distributionCommon = FirebaseConfig.Distribution(
                    serviceCredentialsFilePath = serviceCredentialsFile.name,
                    appId = System.getProperty("FIREBASE_APP_ID"),
                    testerGroups = listOf("testers"),
                    artifactType = "ArtifactType.Apk",
                ),
                distributionBuildType = "internal" to FirebaseConfig.Distribution(
                    serviceCredentialsFilePath = serviceCredentialsFile.name,
                    appId = System.getProperty("FIREBASE_APP_ID_INTERNAL"),
                    testerGroups = listOf("testers"),
                    artifactType = "ArtifactType.Apk",
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent(),
            import = "import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType",
        )
        val givenTagName1 = "v1.0.1-internal"
        val givenTagName2 = "v1.0.2-internal"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleInternal"
        val givenChangelogTask = "generateChangelogInternal"
        val givenAppDistributionTask = "appDistributionUploadInternal"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val generateChangelogResult: BuildResult = projectDir.runTask(givenChangelogTask)
        val distributionResult: BuildResult = projectDir.runTask(givenAppDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/internal")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-internal-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagInternal"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            generateChangelogResult.output.contains("BUILD SUCCESSFUL"),
            "Generate changelog successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Firebase distribution successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `firebase build distribution available with full distribution config, but without assemble and changelog`() {
        val googleServicesFile = projectDir.getFile("app/google-services.json")
        File(System.getProperty("FIREBASE_GOOGLE_SERVICES_FILE_PATH"))
            .copyTo(googleServicesFile, true)
        val serviceCredentialsFile = projectDir.getFile("app/service-credentials.json")
        File(System.getProperty("FIREBASE_SERVICE_CREDENTIALS_FILE_PATH"))
            .copyTo(serviceCredentialsFile, true)

        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            firebaseConfig = FirebaseConfig(
                distributionCommon = FirebaseConfig.Distribution(
                    serviceCredentialsFilePath = serviceCredentialsFile.name,
                    appId = System.getProperty("FIREBASE_APP_ID"),
                    testerGroups = listOf("testers"),
                    artifactType = "ArtifactType.Apk",
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent(),
            import = "import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType",
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenAppDistributionTask = "appDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)

        val distributionResult: BuildResult = projectDir.runTaskWithFail(givenAppDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !distributionResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            !distributionResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            distributionResult.output.contains("BUILD FAILED"),
            "Firebase distribution failed"
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `firebase build bundle distribution not available with full distribution config, but without linked play account`() {
        val googleServicesFile = projectDir.getFile("app/google-services.json")
        File(System.getProperty("FIREBASE_GOOGLE_SERVICES_FILE_PATH"))
            .copyTo(googleServicesFile, true)
        val serviceCredentialsFile = projectDir.getFile("app/service-credentials.json")
        File(System.getProperty("FIREBASE_SERVICE_CREDENTIALS_FILE_PATH"))
            .copyTo(serviceCredentialsFile, true)

        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            firebaseConfig = FirebaseConfig(
                distributionCommon = FirebaseConfig.Distribution(
                    serviceCredentialsFilePath = serviceCredentialsFile.name,
                    appId = System.getProperty("FIREBASE_APP_ID"),
                    testerGroups = listOf("testers"),
                    artifactType = "ArtifactType.Bundle",
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent(),
            import = "import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType",
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "bundleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val givenAppDistributionTask = "appDistributionUploadDebug"
        val git = projectDir.initGit()
        val givenOutputFile = projectDir.getFile("app/build/outputs/bundle/debug/app-debug.aab")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val generateChangelogResult: BuildResult = projectDir.runTask(givenChangelogTask)
        val distributionResult: BuildResult = projectDir.runTaskWithFail(givenAppDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            generateChangelogResult.output.contains("BUILD SUCCESSFUL"),
            "Generate changelog successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD FAILED"),
            "Firebase distribution failed"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `firebase build distribution available with full distribution config with different artifact type`() {
        val googleServicesFile = projectDir.getFile("app/google-services.json")
        File(System.getProperty("FIREBASE_GOOGLE_SERVICES_FILE_PATH"))
            .copyTo(googleServicesFile, true)
        val serviceCredentialsFile = projectDir.getFile("app/service-credentials.json")
        File(System.getProperty("FIREBASE_SERVICE_CREDENTIALS_FILE_PATH"))
            .copyTo(serviceCredentialsFile, true)

        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            firebaseConfig = FirebaseConfig(
                distributionCommon = FirebaseConfig.Distribution(
                    serviceCredentialsFilePath = serviceCredentialsFile.name,
                    appId = System.getProperty("FIREBASE_APP_ID"),
                    testerGroups = listOf("testers"),
                    artifactType = "ArtifactType.Bundle",
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent(),
            import = "import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType",
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val givenAppDistributionTask = "appDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val generateChangelogResult: BuildResult = projectDir.runTask(givenChangelogTask)
        val distributionResult: BuildResult = projectDir.runTaskWithFail(givenAppDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            generateChangelogResult.output.contains("BUILD SUCCESSFUL"),
            "Generate changelog successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD FAILED"),
            "Firebase distribution failed"
        )
        assertTrue(givenOutputFileExists, "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `firebase build distribution available with full distribution config without google service file`() {
        val serviceCredentialsFile = projectDir.getFile("app/service-credentials.json")
        File(System.getProperty("FIREBASE_SERVICE_CREDENTIALS_FILE_PATH"))
            .copyTo(serviceCredentialsFile, true)

        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            firebaseConfig = FirebaseConfig(
                distributionCommon = FirebaseConfig.Distribution(
                    serviceCredentialsFilePath = serviceCredentialsFile.name,
                    appId = System.getProperty("FIREBASE_APP_ID"),
                    testerGroups = listOf("testers"),
                    artifactType = "ArtifactType.Apk",
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent(),
            import = "import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType",
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val givenAppDistributionTask = "appDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)


        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val generateChangelogResult: BuildResult = projectDir.runTask(givenChangelogTask)
        val distributionResult: BuildResult = projectDir.runTask(givenAppDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            generateChangelogResult.output.contains("BUILD SUCCESSFUL"),
            "Generate changelog successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Firebase distribution successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `firebase build distribution not available with full distribution config without generated changelog`() {
        val googleServicesFile = projectDir.getFile("app/google-services.json")
        File(System.getProperty("FIREBASE_GOOGLE_SERVICES_FILE_PATH"))
            .copyTo(googleServicesFile, true)
        val serviceCredentialsFile = projectDir.getFile("app/service-credentials.json")
        File(System.getProperty("FIREBASE_SERVICE_CREDENTIALS_FILE_PATH"))
            .copyTo(serviceCredentialsFile, true)

        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            firebaseConfig = FirebaseConfig(
                distributionCommon = FirebaseConfig.Distribution(
                    serviceCredentialsFilePath = serviceCredentialsFile.name,
                    appId = System.getProperty("FIREBASE_APP_ID"),
                    testerGroups = listOf("testers"),
                    artifactType = "ArtifactType.Apk",
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent(),
            import = "import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType",
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val givenAppDistributionTask = "appDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val distributionResult: BuildResult = projectDir.runTaskWithFail(givenAppDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD FAILED"),
            "Firebase distribution failed"
        )
        assertTrue(givenOutputFileExists, "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `firebase build distribution not available with full distribution config without assemble`() {
        val googleServicesFile = projectDir.getFile("app/google-services.json")
        File(System.getProperty("FIREBASE_GOOGLE_SERVICES_FILE_PATH"))
            .copyTo(googleServicesFile, true)
        val serviceCredentialsFile = projectDir.getFile("app/service-credentials.json")
        File(System.getProperty("FIREBASE_SERVICE_CREDENTIALS_FILE_PATH"))
            .copyTo(serviceCredentialsFile, true)

        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            firebaseConfig = FirebaseConfig(
                distributionCommon = FirebaseConfig.Distribution(
                    serviceCredentialsFilePath = serviceCredentialsFile.name,
                    appId = System.getProperty("FIREBASE_APP_ID"),
                    testerGroups = listOf("testers"),
                    artifactType = "ArtifactType.Apk",
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent(),
            import = "import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType",
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenChangelogTask = "generateChangelogDebug"
        val givenAppDistributionTask = "appDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)

        val generateChangelogResult: BuildResult = projectDir.runTask(givenChangelogTask)
        val distributionResult: BuildResult = projectDir.runTaskWithFail(givenAppDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !generateChangelogResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            generateChangelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            generateChangelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            generateChangelogResult.output.contains("BUILD SUCCESSFUL"),
            "Generate changelog successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD FAILED"),
            "Firebase distribution failed"
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `firebase build distribution available with partial (no tester groups) distribution config`() {
        val googleServicesFile = projectDir.getFile("app/google-services.json")
        File(System.getProperty("FIREBASE_GOOGLE_SERVICES_FILE_PATH"))
            .copyTo(googleServicesFile, true)
        val serviceCredentialsFile = projectDir.getFile("app/service-credentials.json")
        File(System.getProperty("FIREBASE_SERVICE_CREDENTIALS_FILE_PATH"))
            .copyTo(serviceCredentialsFile, true)

        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            firebaseConfig = FirebaseConfig(
                distributionCommon = FirebaseConfig.Distribution(
                    serviceCredentialsFilePath = serviceCredentialsFile.name,
                    appId = System.getProperty("FIREBASE_APP_ID"),
                    testerGroups = null,
                    artifactType = "ArtifactType.Apk",
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent(),
            import = "import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType",
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val givenAppDistributionTask = "appDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val generateChangelogResult: BuildResult = projectDir.runTask(givenChangelogTask)
        val distributionResult: BuildResult = projectDir.runTask(givenAppDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            generateChangelogResult.output.contains("BUILD SUCCESSFUL"),
            "Generate changelog successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Firebase distribution successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")
    }

}

private fun getChangelog(): String {
    return """
[CEB-3243] [And] Mickey tried to fix the loader on Goofy’s form after settings got tangled, and navigation went bonkers
[CEB-3277] [Android] Donald’s transaction history exploded when he peeked into Daisy’s credit card details
    """.trimIndent()
}
