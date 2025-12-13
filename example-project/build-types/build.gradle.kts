import ru.kode.android.build.publish.plugin.core.strategy.FixedApkNamingStrategy
import ru.kode.android.build.publish.plugin.core.strategy.FixedVersionCodeStrategy
import ru.kode.android.build.publish.plugin.core.strategy.FixedVersionNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionedApkNamingStrategy

plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.firebase")
    id("ru.kode.android.build-publish-novo.telegram")
    id("ru.kode.android.build-publish-novo.confluence")
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

buildPublishFoundation {
    output {
        common {
            baseFileName.set("example-base-project-android")
            versionNameStrategy {
                FixedVersionNameStrategy { "version" }
            }
            versionCodeStrategy {
                FixedVersionCodeStrategy { 10000 }
            }
            outputApkNameStrategy {
                FixedApkNamingStrategy { "test" }
            }
        }

        buildVariant("debug") {
            baseFileName.set("example-base-project-android")
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

buildPublishFirebase {
    distribution {
        common {
            serviceCredentialsFile.set(File("Test"))
            appId.set("ru.kode.test.app")
            testerGroups("android-testers")
        }
    }
}

buildPublishTelegram {
    bots {
        common {
            bot("buildPublish") {
                botId.set("313123131231")
                chat("builds") {
                    chatId.set("-00000000")
                }
            }
        }
    }
    distribution {
        common {
            destinationBot {
                botName = "buildPublish"
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
