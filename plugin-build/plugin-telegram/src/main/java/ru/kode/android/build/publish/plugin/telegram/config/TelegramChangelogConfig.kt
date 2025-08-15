package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class TelegramChangelogConfig
    @Inject
    constructor(
        private val objects: ObjectFactory,
    ) {
        abstract val name: String

        /**
         * List of mentioning users for Telegram, can be empty or null
         * For example: ["@aa", "@bb", "@ccc"]
         */
        @get:Input
        abstract val userMentions: SetProperty<String>

        @get:Input
        internal abstract val destinationBots: SetProperty<DestinationBot>

        fun destinationBot(action: Action<DestinationBot>) {
            val destinationBot = objects.newInstance(DestinationBot::class.java)
            action.execute(destinationBot)
            destinationBots.add(destinationBot)
        }
    }
