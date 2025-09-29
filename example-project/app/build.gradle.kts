plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.jira")
    id("ru.kode.android.build-publish-novo.appcenter")
    id("ru.kode.android.build-publish-novo.confluence")
    id("ru.kode.android.build-publish-novo.telegram")
    id("ru.kode.android.build-publish-novo.clickup")
    id("ru.kode.android.build-publish-example.print-tag")
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

buildPublishFoundation {
    outputCommon {
        baseFileName.set("example-base-project-android")
    }
    changelogCommon {
        issueNumberPattern.set("AT-\\d+")
        issueUrlPrefix.set("https://jira.atlassian.com/")
        commitMessageKey.set("CHANGELOG")
    }
}

buildPublishJira {
    auth {
        common {
            baseUrl.set("https://jira.atlassian.com")
            credentials.username.set("test_user_default")
            credentials.password.set("test_password_default")
        }
        buildVariant("release") {
            baseUrl.set("https://jira.atlassian.com")
            credentials.username.set("test_user_release")
            credentials.password.set("test_password_release")
        }
    }
    automation {
        common {
            projectId.set(1111)
            fixVersionPattern.set("fix_%2\$s_%1\$s")
        }
    }
}

buildPublishAppCenter {
    auth {
        common {
            ownerName.set("android-team-kode.ru")
            apiTokenFile.set(File("appcenter-token.txt"))
        }
    }

    distribution {
        common {
            appName.set("Android")
            testerGroups("Collaborators")
        }

        buildVariant("debug") {
            appName.set("AndroidDebug")
            testerGroups("Collaborators")
        }

        buildVariant("release") {
            appName.set("AndroidRelease")
            testerGroups("Collaborators")
        }
    }
}

buildPublishConfluence {
    auth {
        common {
            credentials.username.set("@username")
            credentials.password.set("@password")
        }
    }
    distribution {
        buildVariant("default") {
            pageId.set("123435")
        }
    }
}

buildPublishTelegram {
    bots {
        common {
            bot("buildPublish") {
                botId.set("0000")
                chat("test_chat_A") {
                    chatId = "a"
                    topicId = "1"
                }
                chat("test_chat_B") {
                    chatId = "b"
                    topicId = "2"
                }
            }
        }
    }
    changelog {
        common {
            userMentions("@ivan", "@roman", "@serega")
            destinationBot {
                botName = "buildPublish"
                chatNames("test_chat_A")
            }
        }
    }
}

buildPublishClickUp {
    auth {
        common {
            apiTokenFile = File("clickup-token.txt")
        }
    }
    automation {
        common {
            fixVersionPattern = "fix_%2\$s_%1\$s"
            fixVersionFieldId = "01234567qwerty"
            tagName = "test_tag_name"
        }
    }
}

buildPublishPrintTag {
    messageCommon {
        additionalText.set("Additional text common")
    }
    message {
        buildVariant("armv8MinApi21AlphaRelease") {
            additionalText.set("Additional text Armv8MinApi21AlphaRelease")
        }
    }
}
