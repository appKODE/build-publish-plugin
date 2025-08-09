package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class TelegramBotServerAuthConfig {

    @get:Input
    abstract val authUsername: Property<String>

    @get:Input
    abstract val authPassword: Property<String>

}
