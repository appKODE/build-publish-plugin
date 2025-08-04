package ru.kode.android.build.publish.plugin.telegram.core

import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input

interface TelegramChangelogConfig {
    val name: String

    /**
     * List of mentioning users for Telegram, can be empty or null
     * For example: ["@aa", "@bb", "@ccc"]
     */
    @get:Input
    val userMentions: SetProperty<String>

}
