package ru.kode.android.build.publish.plugin.clickup.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.clickup.config.ClickUpAuthConfig
import ru.kode.android.build.publish.plugin.clickup.config.ClickUpAutomationConfig
import ru.kode.android.build.publish.plugin.core.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishClickUpExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        internal val auth: NamedDomainObjectContainer<ClickUpAuthConfig> =
            objectFactory.domainObjectContainer(ClickUpAuthConfig::class.java)

        internal val automation: NamedDomainObjectContainer<ClickUpAutomationConfig> =
            objectFactory.domainObjectContainer(ClickUpAutomationConfig::class.java)

        fun auth(configurationAction: Action<BaseDomainContainer<ClickUpAuthConfig>>) {
            val container = BaseDomainContainer(auth)
            configurationAction.execute(container)
        }

        fun automation(configurationAction: Action<BaseDomainContainer<ClickUpAutomationConfig>>) {
            val container = BaseDomainContainer(automation)
            configurationAction.execute(container)
        }

        fun authCommon(configurationAction: Action<ClickUpAuthConfig>) {
            common(auth, configurationAction)
        }

        fun automationCommon(configurationAction: Action<ClickUpAutomationConfig>) {
            common(automation, configurationAction)
        }
    }
