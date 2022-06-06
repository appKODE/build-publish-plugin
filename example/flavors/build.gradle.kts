plugins {
    id("com.android.application")
    id("com.google.firebase.appdistribution")
    id("ru.kode.android.build-publish")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "com.example.flavors.android"
        minSdk = 31
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions.add("default")

    productFlavors {

        create("google") {
            dimension = "default"
        }
        create("kode") {
            dimension = "default"
        }
    }
}

buildPublish {
    changelog {
        register("default") {
            issueNumberPattern.set("BASE-\\d+")
            issueUrlPrefix.set("https://jira.exmaple.ru/browse/")
            baseOutputFileName.set("example-base-project-android")
            commitMessageKey.set("CHANGELOG")
        }
    }
    telegram {
        register("default") {
            webhookUrl.set("https://api.telegram.org/%s/sendMessage?chat_id=%s&text=%s&parse_mode=MarkdownV2")
            botId.set("TELEGRAM_BUILD_BOT_ID")
            chatId.set("CHAT_ID")
            userMentions.set(setOf("@ivan", "@roman", "@serega"))
        }
    }
    firebaseDistribution {
        register("default") {
            serviceCredentialsFilePath.set("test-test")
            appId.set("ru.kode.test.app")
            testerGroups.set(setOf("android-testers"))
        }
    }
}
