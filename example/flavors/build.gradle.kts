plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.confluence")
    id("ru.kode.android.build-publish-novo.telegram")
    id("ru.kode.android.build-publish-novo.firebase")
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

buildPublishFoundation {
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
    bot {
        register("default") {
            botId.set("TELEGRAM_BUILD_BOT_ID")
            chatId.set("CHAT_ID")
            topicId.set("OPTIONAL_TOPIC_ID")
        }
    }
    changelog {
        register("default") {
            userMentions.set(setOf("@ivan", "@roman", "@serega"))
        }
    }
}

buildPublishConfluence {
    auth {
        register("default") {
            username.set("@username")
            password.set("@password")
        }
    }

    distribution {
        register("default") {
            pageId.set("123435")
        }
    }
}

buildPublishFirebase {
    distribution {
        register("default") {
            serviceCredentialsFilePath.set("test-test")
            appId.set("ru.kode.test.app")
            testerGroups.set(setOf("android-testers"))
        }
    }
}
