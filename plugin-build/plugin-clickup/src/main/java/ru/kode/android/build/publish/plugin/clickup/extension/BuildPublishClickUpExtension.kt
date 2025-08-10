package ru.kode.android.build.publish.plugin.clickup.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.clickup.config.ClickUpAuthConfig
import ru.kode.android.build.publish.plugin.clickup.config.ClickUpAutomationConfig
import ru.kode.android.build.publish.plugin.core.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishClickUpExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        internal val auth: NamedDomainObjectContainer<ClickUpAuthConfig> =
            objectFactory.domainObjectContainer(ClickUpAuthConfig::class.java)

        internal val automation: NamedDomainObjectContainer<ClickUpAutomationConfig> =
            objectFactory.domainObjectContainer(ClickUpAutomationConfig::class.java)

        val authConfig: (buildName: String) -> ClickUpAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        val authConfigOrNull: (buildName: String) -> ClickUpAuthConfig? = { buildName ->
            auth.getByNameOrNullableCommon(buildName)
        }

        val automationConfig: (buildName: String) -> ClickUpAutomationConfig = { buildName ->
            automation.getByNameOrRequiredCommon(buildName)
        }

        val automationConfigOrNull: (buildName: String) -> ClickUpAutomationConfig? = { buildName ->
            automation.getByNameOrNullableCommon(buildName)
        }

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
