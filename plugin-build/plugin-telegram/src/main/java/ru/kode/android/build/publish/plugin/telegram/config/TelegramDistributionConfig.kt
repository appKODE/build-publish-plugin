package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

abstract class TelegramDistributionConfig @Inject constructor(
    private val objects: ObjectFactory
) {
    abstract val name: String

    @get:Input
    internal abstract val destinationBots: SetProperty<DestinationBot>

    fun destinationBot(action: Action<DestinationBot>) {
        val destinationBot = objects.newInstance(DestinationBot::class.java)
        action.execute(destinationBot)
        destinationBots.add(destinationBot)
    }
}
