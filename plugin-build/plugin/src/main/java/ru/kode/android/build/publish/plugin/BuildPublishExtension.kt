package ru.kode.android.build.publish.plugin

import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

const val EXTENSION_NAME = "buildPublish"

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishExtension @Inject constructor(project: Project) {

    private val objects = project.objects

    /**
     * Message key to collect interested commits
     * For exmaple: CHANGELOG
     */
    val commitMessageKey: Property<String> = objects.property(String::class.java).apply {
        set("CHANGELOG")
    }

    /**
     * Application bundle name for changelog
     * For example: example-base-project-android
     */
    val baseOutputFileName: Property<String> = objects.property(String::class.java)

    /**
     * The path to your service account private key JSON file for Firebase App Distribution
     */
    val distributionServiceCredentialsFilePath: Property<String> = objects.property(String::class.java)

    /**
     * Custom app id for Firebase App Distribution to override google-services.json
     */
    val distributionAppId: Property<String> = objects.property(String::class.java)

    /**
     * Test groups for app distribution
     *
     * For example: [android-testers]
     */
    val distributionTesterGroups: SetProperty<String> =
        objects.setProperty(String::class.java).apply {
            set(setOf("android-testers"))
        }

    /**
     * Artifact type for app distribution
     * For example: APK or AAB
     */
    val distributionArtifactType: Property<String> = objects.property(String::class.java).apply {
        set("APK")
    }

    /**
     * Address of task tracker
     * For example: "https://jira.example.ru/browse/"
     */
    val issueUrlPrefix: Property<String> = objects.property(String::class.java)

    /**
     * How task number formatted
     * For example:  "BASE-\\d+"
     */
    val issueNumberPattern: Property<String> = objects.property(String::class.java)

    /**
     * Config for Telegram changelog sender
     *
     * For example:
     *  webhook_url: "https://api.telegram.org/%s/sendMessage?chat_id=%s&text=%s&parse_mode=MarkdownV2"
     *  bot_id: "TELEGRAM_BUILD_BOT_ID"
     *  chat_id: "CHAT_ID"
     */
    val tgConfig: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    /**
     * List of mentioning users for Telegram, can be empty or null
     * For example: ["@serega", "@valisily"]
     */
    val tgUserMentions: SetProperty<String> = objects.setProperty(String::class.java)

    /**
     * Config for Slack changelog sender
     *
     * For example:
     *  webhook_url: "https://hooks.slack.com/services/111111111/AAAAAAA/DDDDDDD"
     *  icon_url: "https://i.imgur.com/HQTF5FK.png"
     */
    val slackConfig: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    /**
     * List of mentioning users for Slack, can be empty or null
     * For example: ["@aa", "@bb", "@ccc"]
     */
    val slackUserMentions: SetProperty<String> = objects.setProperty(String::class.java)

    /**
     * AppCenter config: owner_name, app_name and api_token_file_path
     * For example:
     *
     * owner_name = android-team.com
     *
     * app_name = YetAnotherTaskApp
     *
     * apiTokenFilePath = /app_center/tokens
     */
    val appCenterConfig: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    /**
     * Distribution group names
     * For example: [internal-testers, client-testers]
     */
    val appCenterDistributionGroups: SetProperty<String> = objects.setProperty(String::class.java)
}
