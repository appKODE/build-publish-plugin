package ru.kode.android.build.publish.plugin.clickup.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.clickup.core.ClickUpAuthConfig
import ru.kode.android.build.publish.plugin.clickup.core.ClickUpAutomationConfig
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishClickUpExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        val auth: NamedDomainObjectContainer<ClickUpAuthConfig> =
            objectFactory.domainObjectContainer(ClickUpAuthConfig::class.java)
        val automation: NamedDomainObjectContainer<ClickUpAutomationConfig> =
            objectFactory.domainObjectContainer(ClickUpAutomationConfig::class.java)

        fun authDefault(configurationAction: Action<ClickUpAuthConfig>) {
            prepareDefault(auth, configurationAction)
        }

        fun automationDefault(configurationAction: Action<ClickUpAutomationConfig>) {
            prepareDefault(automation, configurationAction)
        }
    }
