package ru.kode.android.build.publish.plugin.jira.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.jira.config.JiraAuthConfig
import ru.kode.android.build.publish.plugin.jira.config.JiraAutomationConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishJiraExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        internal val auth: NamedDomainObjectContainer<JiraAuthConfig> =
            objectFactory.domainObjectContainer(JiraAuthConfig::class.java)

        val authConfig: (buildName: String) -> JiraAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        internal val automation: NamedDomainObjectContainer<JiraAutomationConfig> =
            objectFactory.domainObjectContainer(JiraAutomationConfig::class.java)

        fun auth(configurationAction: Action<BaseDomainContainer<JiraAuthConfig>>) {
            val container = BaseDomainContainer(auth)
            configurationAction.execute(container)
        }

        fun automation(configurationAction: Action<BaseDomainContainer<JiraAutomationConfig>>) {
            val container = BaseDomainContainer(automation)
            configurationAction.execute(container)
        }

        fun authAll(configurationAction: Action<JiraAuthConfig>) {
            common(auth, configurationAction)
        }

        fun automationAll(configurationAction: Action<JiraAutomationConfig>) {
            common(automation, configurationAction)
        }
    }
