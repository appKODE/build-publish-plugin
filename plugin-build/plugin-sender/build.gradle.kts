plugins {
    id("kotlin-convention")
    id("plugin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

base {
    archivesName.set("build-publish-novo-sender")
}

dependencies {
    implementation(gradleApi())
    implementation(libs.client.slack)
    implementation(libs.client.telegram)
    implementation(libs.client.nextcloud)
    implementation(libs.client.jira)
    implementation(libs.client.confluence)
    implementation(libs.client.clickup)
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-novo.sender") {
            id = "ru.kode.android.build-publish-novo.sender"
            displayName = "Send messages and files without Android build integration"
            implementationClass = "ru.kode.android.build.publish.plugin.sender.BuildPublishSenderPlugin"
            version = project.version
            description = "Standalone plugin to send messages and files via Slack, Telegram, Nextcloud, Jira, etc."
            tags.set(listOf("slack", "telegram", "notifications", "publish", "sender"))
        }
    }
}
