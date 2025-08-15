package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class DestinationBot
    @Inject
    constructor() {
        @get:Input
        abstract val botName: Property<String>

        @get:Input
        abstract val chatNames: SetProperty<String>
    }
