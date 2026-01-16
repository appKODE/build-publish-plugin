import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType

plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
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

buildPublishFoundation {
    output {
        common {
            baseFileName.set("example-base-project-android")
        }

        buildVariant("x86MinApi21AlphaDebug") {
            baseFileName.set("example-base-project-android")
            buildTagPattern {
                literal("cabinet")
                separator("+")
                anyBeforeDot()
                buildVersion()
                separator("-")
                buildVariantName()
            }
        }
    }
    changelog {
        common {
            issueNumberPattern.set("BASE-\\d+")
            issueUrlPrefix.set("https://jira.exmaple.ru/browse/")
            commitMessageKey.set("CHANGELOG")
        }
    }
}

buildPublishFirebase {
    distribution {
        common {
            serviceCredentialsFile.set(File("Test"))
            appId.set("ru.kode.test.app")
            artifactType.set(ArtifactType.Apk)
            testerGroups("android-testers")
        }
    }
}

buildPublishSlack {
    bot {
        common {
            webhookUrl.set("https://hooks.slack.com/services/111111111/AAAAAAA/DDDDDDD")
            iconUrl.set("https://i.imgur.com/HQTF5FK.png")
            uploadApiTokenFile.set(File("Test"))
        }
    }
    changelog {
        common {
            userMentions("@aa", "@bb", "@cc")
            attachmentColor.set("#ffffff")
        }
    }
    distributionCommon {
        destinationChannel("builds")
    }
}
