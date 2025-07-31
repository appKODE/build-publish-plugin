plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish.base")
    id("ru.kode.android.build-publish.firebase")
    id("ru.kode.android.build-publish.telegram")
    id("ru.kode.android.build-publish.confluence")
}

android {
    namespace = "ru.kode.android.build.types.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.build.types.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

buildPublishBase {
    output {
        register("default") {
            baseFileName.set("example-base-project-android")
        }

        register("debug") {
            baseFileName.set("example-base-project-android")
            useVersionsFromTag.set(false)
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

buildPublishFirebase {
    firebaseDistribution {
        register("default") {
            serviceCredentialsFilePath.set("test-test")
            appId.set("ru.kode.test.app")
            testerGroups.set(setOf("android-testers"))
        }
    }
}
buildPublishTelegram {
    telegram {
        register("default") {
            botId.set("313123131231")
            chatId.set("-00000000")
            userMentions.set(emptyList())
            uploadBuild.set(true)
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
