package ru.kode.android.build.publish.plugin.clickup.extension

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.clickup.config.ClickUpAuthConfig
import ru.kode.android.build.publish.plugin.clickup.config.ClickUpAutomationConfig
import ru.kode.android.build.publish.plugin.clickup.messages.needToProvideAuthConfigMessage
import ru.kode.android.build.publish.plugin.clickup.messages.needToProvideAutomationConfigMessage
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpAutomationTaskParams
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpTasksRegistrar
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import javax.inject.Inject

/**
 * Extension for configuring the ClickUp plugin in a Gradle project.
 *
 * This class provides configuration options for authenticating with ClickUp's API
 * and defining automation rules for task management during the build process.
 *
 * @see ClickUpAuthConfig For authentication configuration options
 * @see ClickUpAutomationConfig For automation configuration options
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishClickUpExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        /**
         * Container for ClickUp authentication configurations.
         *
         * This container holds named configurations for authenticating with the ClickUp API.
         * Each configuration is typically associated with a build variant or environment.
         */
        internal val auth: NamedDomainObjectContainer<ClickUpAuthConfig> =
            objectFactory.domainObjectContainer(ClickUpAuthConfig::class.java)

        /**
         * Container for ClickUp automation configurations.
         *
         * This container holds named configurations for automating ClickUp tasks
         * during the build process. Each configuration is typically associated
         * with a build variant or environment.
         */
        internal val automation: NamedDomainObjectContainer<ClickUpAutomationConfig> =
            objectFactory.domainObjectContainer(ClickUpAutomationConfig::class.java)

        /**
         * Retrieves the authentication configuration for the specified build variant.
         *
         * @param buildName The name of the build variant
         *
         * @return The matching authentication configuration
         * @throws UnknownDomainObjectException If no matching configuration is found
         */
        val authConfig: (buildName: String) -> ClickUpAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves the authentication configuration for the specified build variant, if it exists.
         *
         * @param buildName The name of the build variant
         *
         * @return The matching authentication configuration, or null if not found
         */
        val authConfigOrNull: (buildName: String) -> ClickUpAuthConfig? = { buildName ->
            auth.getByNameOrNullableCommon(buildName)
        }

        /**
         * Retrieves the automation configuration for the specified build variant.
         *
         * @param buildName The name of the build variant
         *
         * @return The matching automation configuration
         * @throws UnknownDomainObjectException If no matching configuration is found
         */
        val automationConfig: (buildName: String) -> ClickUpAutomationConfig = { buildName ->
            automation.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves the automation configuration for the specified build variant, if it exists.
         *
         * @param buildName The name of the build variant
         *
         * @return The matching automation configuration, or null if not found
         */
        val automationConfigOrNull: (buildName: String) -> ClickUpAutomationConfig? = { buildName ->
            automation.getByNameOrNullableCommon(buildName)
        }

        /**
         * Configures authentication settings for the ClickUp plugin.
         *
         * This method provides a DSL for configuring one or more authentication profiles.
         *
         * @param configurationAction The configuration action to apply
         * @see ClickUpAuthConfig For available configuration options
         */
        fun auth(configurationAction: Action<BuildPublishDomainObjectContainer<ClickUpAuthConfig>>) {
            val container = BuildPublishDomainObjectContainer(auth)
            configurationAction.execute(container)
        }

        /**
         * Configures automation rules for the ClickUp plugin.
         *
         * This method provides a DSL for configuring one or more automation profiles.
         *
         * @param configurationAction The configuration action to apply
         * @see ClickUpAutomationConfig For available configuration options
         */
        fun automation(configurationAction: Action<BuildPublishDomainObjectContainer<ClickUpAutomationConfig>>) {
            val container = BuildPublishDomainObjectContainer(automation)
            configurationAction.execute(container)
        }

        /**
         * Configures common authentication settings that apply to all build variants.
         *
         * This is a convenience method for applying the same authentication settings
         * to all build variants. These settings can be overridden by variant-specific configurations.
         *
         * @param configurationAction The configuration action to apply
         */
        fun authCommon(configurationAction: Action<ClickUpAuthConfig>) {
            common(auth, configurationAction)
        }

        /**
         * Configures common automation rules that apply to all build variants.
         *
         * This is a convenience method for applying the same automation rules
         * to all build variants. These rules can be overridden by variant-specific configurations.
         *
         * @param configurationAction The configuration action to apply
         */
        fun automationCommon(configurationAction: Action<ClickUpAutomationConfig>) {
            common(automation, configurationAction)
        }

        /**
         * Configures the ClickUp tasks for the given build variant.
         *
         * This method is called by the build system to set up the ClickUp automation tasks
         * for a specific build variant. It retrieves the appropriate configuration and
         * registers the necessary tasks with the project.
         *
         * @param project The Gradle project
         * @param input The extension input containing build variant and changelog information
         */
        override fun configure(
            project: Project,
            input: ExtensionInput,
        ) {
            val variantName = input.buildVariant.name

            if (auth.isEmpty()) {
                throw GradleException(needToProvideAuthConfigMessage(variantName))
            }

            val automationConfig = automationConfigOrNull(input.buildVariant.name)
                ?: throw GradleException(needToProvideAutomationConfigMessage(variantName))

            ClickUpTasksRegistrar.registerAutomationTask(
                project = project,
                automationConfig = automationConfig,
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
