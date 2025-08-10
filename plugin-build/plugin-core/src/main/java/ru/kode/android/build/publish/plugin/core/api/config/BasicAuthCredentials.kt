package ru.kode.android.build.publish.plugin.core.api.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class BasicAuthCredentials {

    @get:Input
    abstract val username: Property<String>

    @get:Input
    abstract val password: Property<String>

}
