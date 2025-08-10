package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import javax.inject.Inject

abstract class TelegramBotConfig @Inject constructor(
    objects: ObjectFactory
) {
    abstract val name: String

    /**
     * Telegram bot id to post changelog in chat
     */
    @get:Input
    abstract val botId: Property<String>

    @get:Input
    @get:Optional
    abstract val botServerBaseUrl: Property<String>

    @get:Nested
    @get:Optional
    val botServerAuth: TelegramBotServerAuthConfig =
        objects.newInstance(TelegramBotServerAuthConfig::class.java)

    internal val chats: NamedDomainObjectContainer<TelegramChatConfig> =
        objects.domainObjectContainer(TelegramChatConfig::class.java)

    fun chat(chatName: String, action: Action<TelegramChatConfig>) {
        chats.register(chatName, action)
    }
}
