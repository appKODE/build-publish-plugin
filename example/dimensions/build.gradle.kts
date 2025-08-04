plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.base")
    id("ru.kode.android.build-publish-novo.slack")
    id("ru.kode.android.build-publish-novo.firebase")
}

android {
    namespace = "ru.kode.android.dimensions.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.dimensions.android"
        minSdk = 26
        targetSdk = 34
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

buildPublishBase {
    output {
        register("default") {
            baseFileName.set("example-base-project-android")
        }

        register("x86MinApi21AlphaDebug") {
            baseFileName.set("example-base-project-android")
            buildTagPattern.set("cabinet\\+.+\\.(\\d+)-x86MinApi21AlphaDebug")
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
    distribution {
        register("default") {
            serviceCredentialsFilePath.set("test-test")
            appId.set("ru.kode.test.app")
            testerGroups.set(setOf("android-testers"))
        }
    }
}

buildPublishSlack {
    bot {
        register("default") {
            webhookUrl.set("https://hooks.slack.com/services/111111111/AAAAAAA/DDDDDDD")
            iconUrl.set("https://i.imgur.com/HQTF5FK.png")
        }
    }
    changelog {
        register("default") {
            userMentions.set(setOf("@aa", "@bb", "@cc"))
            attachmentColor.set("#ffffff")
        }
    }
}
