plugins {
    id("com.android.application")
    id("com.google.firebase.appdistribution")
    id("ru.kode.android.build-publish")
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.flavors.android"
        minSdk = 31
        targetSdk = 33
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
    output {
        register("default") {
            baseFileName.set("flavors-example")
        }
    }
    changelog {
        register("default") {
            issueNumberPattern.set("BASE-\\d+")
            issueUrlPrefix.set("https://jira.exmaple.ru/browse/")
            commitMessageKey.set("CHANGELOG")
        }
    }
    telegram {
        register("default") {
            botId.set("TELEGRAM_BUILD_BOT_ID")
            chatId.set("CHAT_ID")
            topicId.set("OPTIONAL_TOPIC_ID")
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
