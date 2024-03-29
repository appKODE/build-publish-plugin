plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish")
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.build.types.android"
        minSdk = 31
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }
}

buildPublish {
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
    firebaseDistribution {
        register("default") {
            serviceCredentialsFilePath.set("test-test")
            appId.set("ru.kode.test.app")
            testerGroups.set(setOf("android-testers"))
        }
    }
}
