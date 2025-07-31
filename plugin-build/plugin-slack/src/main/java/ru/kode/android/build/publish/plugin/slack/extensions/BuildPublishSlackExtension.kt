package ru.kode.android.build.publish.plugin.slack.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.slack.core.SlackConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishSlackExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val slack: NamedDomainObjectContainer<SlackConfig> =
            objectFactory.domainObjectContainer(SlackConfig::class.java)

    }
