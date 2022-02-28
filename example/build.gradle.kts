plugins {
    id("com.android.application")
    id("com.google.firebase.appdistribution")
    id("ru.kode.android.firebase-publish-plugin")
}

android {
    buildTypes {
        debug {

        }

        release {

        }
    }

    flavorDimensions.add("default")
    productFlavors {
        create("google") {
            dimension = "default"
        }
        create("beta") {
            dimension = "default"
        }
    }
}

firebasePublishConfig {
    commitMessageKey.set("CHANGELOG")
    distributionServiceKey.set("FIREBASE_DISTRIBUTION_SERVICE_KEY")
    distributionTesterGroups.set(setOf("android-testers"))

    baseOutputFileName.set("example-base-project-android")
    issueUrlPrefix.set("https://jira.exmaple.ru/browse/")
    issueNumberPattern.set("BASE-\\d+")
    tgConfig.set(
        mapOf(
            "webhook_url" to "https://api.telegram.org/%s/sendMessage?chat_id=%s&text=%s&parse_mode=MarkdownV2",
            "bot_id" to "TELEGRAM_BUILD_BOT_ID",
            "chat_id" to "CHAT_ID"
        )
    )
    tgUserMentions.set(
        setOf(
            "@ivan",
            "@roman",
            "@serega",
        )
    )
    slackConfig.set(
        mapOf(
            "webhook_url" to "https://hooks.slack.com/services/111111111/AAAAAAA/DDDDDDD",
            "icon_url" to "https://i.imgur.com/HQTF5FK.png",
        )
    )
    slackUserMentions.set(
        setOf(
            "@aa",
            "@bb",
            "@cc"
        )
    )
}
