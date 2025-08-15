package ru.kode.android.build.publish.plugin.clickup.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.clickup.config.ClickUpAuthConfig
import ru.kode.android.build.publish.plugin.clickup.config.ClickUpAutomationConfig
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpAutomationTaskParams
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpTasksRegistrar
import ru.kode.android.build.publish.plugin.core.api.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishClickUpExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
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

        override fun configure(
            project: Project,
            input: ExtensionInput,
        ) {
            val buildVariantConfig = automationConfig(input.buildVariant.name)

            ClickUpTasksRegistrar.registerAutomationTask(
                project = project,
                automationConfig = buildVariantConfig,
                params =
                    ClickUpAutomationTaskParams(
                        buildVariant = input.buildVariant,
                        issueNumberPattern = input.changelog.issueNumberPattern,
                        changelogFile = input.changelog.file,
                        lastBuildTagFile = input.output.lastBuildTagFile,
                    ),
            )
        }
    }
