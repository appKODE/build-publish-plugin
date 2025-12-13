package ru.kode.android.build.publish.plugin.jira.extension

import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.jira.config.JiraAuthConfig
import ru.kode.android.build.publish.plugin.jira.config.JiraAutomationConfig
import ru.kode.android.build.publish.plugin.jira.task.JiraAutomationTaskParams
import ru.kode.android.build.publish.plugin.jira.task.JiraTasksRegistrar
import javax.inject.Inject

/**
 * Extension for configuring Jira integration in the build process.
 *
 * This extension provides configuration options for connecting to Jira and
 * automating Jira-related tasks during the build. It supports multiple
 * authentication configurations and automation rules.
 *
 * @see JiraAuthConfig For authentication configuration options
 * @see JiraAutomationConfig For automation rule configuration options
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishJiraExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {

        private val logger: Logger = Logging.getLogger(this::class.java)

         /**
         * Container for Jira authentication configurations.
         *
         * Each configuration defines how to authenticate with a Jira instance.
         * Multiple configurations can be defined for different environments.
         */
        internal val auth: NamedDomainObjectContainer<JiraAuthConfig> =
            objectFactory.domainObjectContainer(JiraAuthConfig::class.java)

        /**
         * Container for Jira automation rule configurations.
         *
         * Each configuration defines rules for automating Jira workflows
         * during the build process.
         */
        internal val automation: NamedDomainObjectContainer<JiraAutomationConfig> =
            objectFactory.domainObjectContainer(JiraAutomationConfig::class.java)

        /**
         * Retrieves a Jira authentication configuration by name, throwing an exception if not found.
         *
         * @param buildName The name of the build variant or configuration
         * @return The matching [JiraAuthConfig]
         * @throws UnknownDomainObjectException If no configuration exists with the given name
         */
        val authConfig: (buildName: String) -> JiraAuthConfig = { buildName ->
            auth.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves a Jira authentication configuration by name, returning null if not found.
         *
         * @param buildName The name of the build variant or configuration
         * @return The matching [JiraAuthConfig], or null if not found
         */
        val authConfigOrNull: (buildName: String) -> JiraAuthConfig? = { buildName ->
            auth.getByNameOrNullableCommon(buildName)
        }

        /**
         * Retrieves a Jira automation configuration by name, throwing an exception if not found.
         *
         * @param buildName The name of the build variant or configuration
         * @return The matching [JiraAutomationConfig]
         * @throws UnknownDomainObjectException If no configuration exists with the given name
         */
        val automationConfig: (buildName: String) -> JiraAutomationConfig = { buildName ->
            automation.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves a Jira automation configuration by name, returning null if not found.
         *
         * @param buildName The name of the build variant or configuration
         * @return The matching [JiraAutomationConfig], or null if not found
         */
        val automationConfigOrNull: (buildName: String) -> JiraAutomationConfig? = { buildName ->
            automation.getByNameOrNullableCommon(buildName)
        }

        /**
         * Configures Jira authentication settings.
         *
         * @param configurationAction The configuration action to apply to the auth container
         * @see JiraAuthConfig For available configuration options
         */
        fun auth(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationAction: Action<BuildPublishDomainObjectContainer<JiraAuthConfig>>
        ) {
            val container = BuildPublishDomainObjectContainer(auth)
            configurationAction.execute(container)
        }

        /**
         * Configures Jira automation rules.
         *
         * @param configurationAction The configuration action to apply to the automation container
         * @see JiraAutomationConfig For available configuration options
         */
        fun automation(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationAction: Action<BuildPublishDomainObjectContainer<JiraAutomationConfig>>
        ) {
            val container = BuildPublishDomainObjectContainer(automation)
            configurationAction.execute(container)
        }

        /**
         * Applies configuration to all Jira authentication settings.
         *
         * @param configurationAction The configuration action to apply to all auth configurations
         */
        fun authCommon(configurationAction: Action<JiraAuthConfig>) {
            common(auth, configurationAction)
        }

        /**
         * Applies configuration to all Jira automation rules.
         *
         * @param configurationAction The configuration action to apply to all automation configurations
         */
        fun automationCommon(configurationAction: Action<JiraAutomationConfig>) {
            common(automation, configurationAction)
        }

        /**
         * Configures Jira tasks for the given project and build variant.
         *
         * This method is called by the build system to set up Jira-related tasks
         * for each build variant.
         *
         * @param project The target project
         * @param input The extension input containing build variant information
         */
        override fun configure(
            project: Project,
            input: ExtensionInput,
        ) {

            val variantName = input.buildVariant.name

            if (auth.isEmpty()) {
                throw GradleException(
                    "Need to provide Auth config for `$variantName` or `common`. " +
                    "It's required to run Jira plugin. " +
                    "Please check that you have 'auth' block in your build script " +
                    "and that it's not empty. "
                )
            }

            val automationConfig = automationConfigOrNull(variantName)
                ?: throw GradleException(
                    "Need to provide Automation config for `$variantName` or `common`. " +
                    "Please check that you have 'automation' block in your build script " +
                    "and that it's not empty. "
                )

            JiraTasksRegistrar.registerAutomationTask(
                project = project,
                automationConfig = automationConfig,
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
