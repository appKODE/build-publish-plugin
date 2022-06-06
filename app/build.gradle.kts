plugins {
    id("com.android.application")
    id("com.google.firebase.appdistribution")
    id("ru.kode.android.build-publish")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "com.example.dimensions.android"
        minSdk = 21
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"
    }

    productFlavors {
        flavorDimensions += listOf("abi", "api", "version")

        create("x86") {
            dimension = "abi"
        }
        create("armv8") {
            dimension = "abi"
        }

        create("minApi21") {
            dimension = "api"
        }
        create("minApi24") {
            dimension = "api"
        }

        create("alpha") {
            dimension = "version"
        }
        create("beta") {
            dimension = "version"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("com.google.android.material:material:1.4.0")
}

buildPublish {
    commitMessageKey.set("CHANGELOG")
    distributionAppId.set("ru.kode.test.app")

    baseOutputFileName.set("example-beta-debug")
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

    appCenterConfig.set(
        mapOf(
            "owner_name" to "android-team-kode.ru",
            "app_name" to "Android",
            "api_token_file_path" to "token"
        )
    )

    appCenterDistributionGroups.set(setOf("Collaborators"))
}
