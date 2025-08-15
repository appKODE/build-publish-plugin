package ru.kode.android.build.publish.plugin.jira.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.jira.config.JiraAuthConfig
import ru.kode.android.build.publish.plugin.jira.config.JiraAutomationConfig
import ru.kode.android.build.publish.plugin.jira.task.JiraAutomationTaskParams
import ru.kode.android.build.publish.plugin.jira.task.JiraTasksRegistrar
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishJiraExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        internal val auth: NamedDomainObjectContainer<JiraAuthConfig> =
            objectFactory.domainObjectContainer(JiraAuthConfig::class.java)

        internal val automation: NamedDomainObjectContainer<JiraAutomationConfig> =
            objectFactory.domainObjectContainer(JiraAutomationConfig::class.java)

        val authConfig: (buildName: String) -> JiraAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        val authConfigOrNull: (buildName: String) -> JiraAuthConfig? = { buildName ->
            auth.getByNameOrNullableCommon(buildName)
        }

        val automationConfig: (buildName: String) -> JiraAutomationConfig = { buildName ->
            automation.getByNameOrRequiredCommon(buildName)
        }

        val automationConfigOrNull: (buildName: String) -> JiraAutomationConfig? = { buildName ->
            automation.getByNameOrNullableCommon(buildName)
        }

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

        override fun configure(
            project: Project,
            input: ExtensionInput,
        ) {
            val buildVariantConfig = automationConfig(input.buildVariant.name)
            JiraTasksRegistrar.registerAutomationTask(
                project = project,
                automationConfig = buildVariantConfig,
                params =
                    JiraAutomationTaskParams(
                        buildVariant = input.buildVariant,
                        issueNumberPattern = input.changelog.issueNumberPattern,
                        changelogFile = input.changelog.file,
                        lastBuildTagFile = input.output.lastBuildTagFile,
                    ),
            )
        }
    }
