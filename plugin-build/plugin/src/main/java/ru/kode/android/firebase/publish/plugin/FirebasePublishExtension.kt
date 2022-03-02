package ru.kode.android.firebase.publish.plugin

import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

const val EXTENSION_NAME = "firebasePublishConfig"

@Suppress("UnnecessaryAbstractClass")
abstract class FirebasePublishExtension @Inject constructor(project: Project) {

    private val objects = project.objects

    /**
     * For some cases can be useful to pass initial build number
     * For exmaple: 71261
     */
    val initialBuildNumber: Property<Int> = objects.property(Int::class.java).apply {
        // TODO Add logic to set from configs
        set(0)
    }

    /**
     * Message key to collect interested commits
     * For exmaple: CHANGELOG
     */
    val commitMessageKey: Property<String> = objects.property(String::class.java).apply {
        // TODO Add logic to set from configs
        set("CHANGELOG")
    }

    /**
     * Application bundle name for changelog
     * For example: example-base-project-android
     */
    val baseOutputFileName: Property<String> = objects.property(String::class.java)

    /**
     * Name of CI env variable, holding Firebase service key
     * For example: FIREBASE_DISTRIBUTION_SERVICE_KEY
     */
    val distributionServiceKey: Property<String> = objects.property(String::class.java).apply {
        // TODO Add logic to set from configs
        set("FIREBASE_DISTRIBUTION_SERVICE_KEY")
    }

    /**
     * Internal test groups for app distribution
     *
     * For example: [android-testers]
     */
    val distributionTesterGroups: SetProperty<String> =
        objects.setProperty(String::class.java).apply {
            // TODO Add logic to set from configs
            set(setOf("android-testers"))
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
}
