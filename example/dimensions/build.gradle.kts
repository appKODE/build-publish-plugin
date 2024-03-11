plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish")
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.dimensions.android"
        minSdk = 31
        targetSdk = 33
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

buildPublish {
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
    slack {
        register("default") {
            webhookUrl.set("https://hooks.slack.com/services/111111111/AAAAAAA/DDDDDDD")
            iconUrl.set("https://i.imgur.com/HQTF5FK.png")
            userMentions.set(setOf("@aa", "@bb", "@cc"))
            attachmentColor.set("#ffffff")
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
