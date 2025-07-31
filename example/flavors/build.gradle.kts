plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish.base")
    id("ru.kode.android.build-publish.confluence")
    id("ru.kode.android.build-publish.telegram")
    id("ru.kode.android.build-publish.firebase")
}

android {
    namespace = "ru.kode.android.flavors.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.flavors.android"
        minSdk = 26
        targetSdk = 34
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

buildPublishBase {
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
}

buildPublishTelegram {
    telegram {
        register("default") {
            botId.set("TELEGRAM_BUILD_BOT_ID")
            chatId.set("CHAT_ID")
            topicId.set("OPTIONAL_TOPIC_ID")
            userMentions.set(setOf("@ivan", "@roman", "@serega"))
        }
    }
}

buildPublishConfluence {
    confluence {
        register("default") {
            username.set("@username")
            password.set("@password")
            pageId.set("123435")
        }
    }
}

buildPublishFirebase {
    firebaseDistribution {
        register("default") {
            serviceCredentialsFilePath.set("test-test")
            appId.set("ru.kode.test.app")
            testerGroups.set(setOf("android-testers"))
        }
    }
}
