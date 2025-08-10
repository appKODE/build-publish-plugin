import ru.kode.android.build.publish.plugin.core.util.buildType
import ru.kode.android.build.publish.plugin.core.util.common

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
        }

        buildType("debug") {
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
            serviceCredentialsFilePath.set("test-test")
            appId.set("ru.kode.test.app")
            testerGroups.set(setOf("android-testers"))
        }
    }
}

buildPublishTelegram {
    bot {
        common {
            botId.set("313123131231")
            //chat.chatId.set("-00000000")
        }
    }
    distribution {
        common {
            uploadBuild.set(true)
        }
    }
}

buildPublishConfluence {
    auth {
        common {
            username.set("@username")
            password.set("@password")
        }
    }

    distribution {
        common {
            pageId.set("123435")
        }
    }
}
