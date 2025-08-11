package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class TelegramBotsConfig @Inject constructor(
    objects: ObjectFactory,
) : Named {

    internal val bots: NamedDomainObjectContainer<TelegramBotConfig> =
        objects.domainObjectContainer(TelegramBotConfig::class.java)

    fun bot(botName: String, action: Action<TelegramBotConfig>) {
        bots.register(botName, action)
    }
}
