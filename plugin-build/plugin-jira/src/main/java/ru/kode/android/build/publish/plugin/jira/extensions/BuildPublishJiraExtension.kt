package ru.kode.android.build.publish.plugin.jira.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.extension.BaseExtension
import ru.kode.android.build.publish.plugin.jira.core.JiraAuthConfig
import ru.kode.android.build.publish.plugin.jira.core.JiraAutomationConfig
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishJiraExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BaseExtension() {
        val auth: NamedDomainObjectContainer<JiraAuthConfig> =
            objectFactory.domainObjectContainer(JiraAuthConfig::class.java)
        val automation: NamedDomainObjectContainer<JiraAutomationConfig> =
            objectFactory.domainObjectContainer(JiraAutomationConfig::class.java)

        fun authDefault(configurationAction: Action<JiraAuthConfig>) {
            prepareDefault(auth, configurationAction)
        }

        fun automationDefault(configurationAction: Action<JiraAutomationConfig>) {
            prepareDefault(automation, configurationAction)
        }
}
