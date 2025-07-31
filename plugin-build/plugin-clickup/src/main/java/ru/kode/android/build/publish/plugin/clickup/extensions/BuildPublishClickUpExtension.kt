package ru.kode.android.build.publish.plugin.clickup.extensions

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.clickup.core.ClickUpConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishClickUpExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val clickUp: NamedDomainObjectContainer<ClickUpConfig> =
            objectFactory.domainObjectContainer(ClickUpConfig::class.java)
    }
