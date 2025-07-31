package ru.kode.android.build.publish.plugin.confluence.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.confluence.core.ConfluenceConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishConfluenceExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val confluence: NamedDomainObjectContainer<ConfluenceConfig> =
            objectFactory.domainObjectContainer(ConfluenceConfig::class.java)
    }
