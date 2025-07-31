plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.base")
    id("ru.kode.android.build-publish-novo.jira")
    id("ru.kode.android.build-publish-novo.appcenter")
    id("ru.kode.android.build-publish-novo.confluence")
    id("ru.kode.android.build-publish-novo.telegram")
    id("ru.kode.android.build-publish-novo.clickup")
}

android {
    namespace = "ru.kode.android.app"
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

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("com.google.android.material:material:1.4.0")
}

buildPublishBase {
    output {
        register("default") {
            baseFileName.set("example-base-project-android")
        }

        register("armv8MinApi21AlphaDebug") {
            baseFileName.set("example-base-project-android")
            useVersionsFromTag.set(false)
        }
    }
    changelog {
        register("default") {
            issueNumberPattern.set("AT-\\d+")
            issueUrlPrefix.set("https://jira.exmaple.ru/browse/")
            commitMessageKey.set("CHANGELOG")
        }
    }
}

buildPublishJira {
    jira {
        register("default") {
            baseUrl.set("https://jira.exmaple.ru")
            authUsername.set("test_user")
            authPassword.set("test_password")
            projectId.set(1111)
            fixVersionPattern.set("fix_%2\$s_%1\$s")
        }
    }
}

buildPublishAppCenter {
    appCenterDistribution {
        register("default") {
            appName.set("Android")
            ownerName.set("android-team-kode.ru")
            apiTokenFile.set(File("appcenter-token.txt"))
            testerGroups.set(setOf("Collaborators"))
        }

        register("debug") {
            appName.set("AndroidDebug")
            ownerName.set("android-team-kode.ru")
            apiTokenFile.set(File("appcenter-token.txt"))
            testerGroups.set(setOf("Collaborators"))
        }

        register("release") {
            appName.set("AndroidRelease")
            ownerName.set("android-team-kode.ru")
            apiTokenFile.set(File("appcenter-token.txt"))
            testerGroups.set(setOf("Collaborators"))
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

buildPublishTelegram {
    telegram {
        register("default") {
            botId.set("0000")
            chatId.set("0000")
            topicId.set("0000")
            userMentions.set(setOf("@ivan", "@roman", "@serega"))
        }
    }
}

buildPublishClickUp {
    clickUp {
        register("default") {
            apiTokenFile = File("clickup-token.txt")
            fixVersionPattern = "fix_%2\$s_%1\$s"
            fixVersionFieldId = "01234567qwerty"
            tagName = "test_tag_name"
        }
    }
}
