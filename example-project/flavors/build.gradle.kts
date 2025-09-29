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
        common {
            baseFileName.set("flavors-example")
        }

        buildVariant("debug") {
            baseFileName.set("flavors-example")
            useVersionsFromTag.set(false)
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

buildPublishTelegram {
    bots {
        common {
            bot("buildPublisher") {
                botId.set("TELEGRAM_BUILD_BOT_ID")

                chat("builds") {
                    chatId.set("CHAT_ID")
                    topicId.set("OPTIONAL_TOPIC_ID")
                }
            }
        }
    }
    changelog {
        common {
            userMentions("@ivan", "@roman", "@serega")

            destinationBot {
                botName = "buildPublisher"
                chatNames("builds")
            }
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
        common {
            pageId.set("123435")
        }
    }
}

buildPublishFirebase {
    distribution {
        common {
            serviceCredentialsFile.set(File("Test"))
            appId.set("ru.kode.test.app")
            testerGroups("android-testers")
        }
    }
}
