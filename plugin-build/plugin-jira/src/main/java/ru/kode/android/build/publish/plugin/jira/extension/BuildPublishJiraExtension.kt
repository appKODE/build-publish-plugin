package ru.kode.android.build.publish.plugin.jira.extension

import com.android.build.api.variant.ApplicationVariant
import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.jira.config.JiraAutomationConfig
import ru.kode.android.build.publish.plugin.jira.config.JiraEndpointsConfig
import ru.kode.android.build.publish.plugin.jira.messages.needToProvideAutomationConfigMessage
import ru.kode.android.build.publish.plugin.jira.messages.needToProvideEndpointsConfigMessage
import ru.kode.android.build.publish.plugin.jira.task.JiraAutomationTaskParams
import ru.kode.android.build.publish.plugin.jira.task.JiraTasksRegistrar
import javax.inject.Inject

/**
 * Extension for configuring Jira integration in the build process.
 *
 * This extension provides configuration options for connecting to Jira and
 * automating Jira-related tasks during the build. It supports multiple
 * endpoint configurations and automation rules.
 *
 * @see JiraEndpointsConfig For endpoint configuration options
 * @see JiraAutomationConfig For automation rule configuration options
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BuildPublishJiraExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {
        /**
         * Container for Jira endpoints configurations.
         *
         * Each configuration defines a set of named endpoints items for a build variant.
         */
        internal val endpoints: NamedDomainObjectContainer<JiraEndpointsConfig> =
            objectFactory.domainObjectContainer(JiraEndpointsConfig::class.java)

        /**
         * Container for Jira automation rule configurations.
         *
         * Each configuration defines rules for automating Jira workflows
         * during the build process.
         */
        internal val automation: NamedDomainObjectContainer<JiraAutomationConfig> =
            objectFactory.domainObjectContainer(JiraAutomationConfig::class.java)

        /**
         * Retrieves a Jira endpoint configuration by name, throwing an exception if not found.
         *
         * @param buildName The name of the build variant or configuration
         * @return The matching [JiraEndpointsConfig]
         * @throws UnknownDomainObjectException If no configuration exists with the given name
         */
        val endpointsConfig: (buildName: String) -> JiraEndpointsConfig = { buildName ->
            endpoints.getByNameOrRequiredCommon(buildName)
        }

        /**
         * Retrieves a Jira endpoint configuration by name, returning null if not found.
         *
         * @param buildName The name of the build variant or configuration
         * @return The matching [JiraEndpointsConfig], or null if not found
         */
        val endpointsConfigOrNull: (buildName: String) -> JiraEndpointsConfig? = { buildName ->
            endpoints.getByNameOrNullableCommon(buildName)
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
         * Configures Jira endpoint settings.
         *
         * @param configurationAction The configuration action to apply to the endpoints container
         * @see JiraEndpointsConfig For available configuration options
         */
        fun endpoints(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationAction: Action<BuildPublishDomainObjectContainer<JiraEndpointsConfig>>,
        ) {
            val container = BuildPublishDomainObjectContainer(endpoints)
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
            configurationAction: Action<BuildPublishDomainObjectContainer<JiraAutomationConfig>>,
        ) {
            val container = BuildPublishDomainObjectContainer(automation)
            configurationAction.execute(container)
        }

        /**
         * Configures Jira automation rules using a Groovy closure.
         *
         * @param configurationClosure The Groovy closure to apply to the automation container
         * @see JiraAutomationConfig For available configuration options
         */
        fun automation(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationClosure: Closure<in BuildPublishDomainObjectContainer<JiraAutomationConfig>>,
        ) {
            val container = BuildPublishDomainObjectContainer(automation)
            configureGroovy(configurationClosure, container)
        }

        /**
         * Applies configuration to all Jira endpoint settings.
         *
         * @param configurationAction The configuration action to apply to all endpoint configurations
         */
        fun endpointsCommon(configurationAction: Action<JiraEndpointsConfig>) {
            common(endpoints, configurationAction)
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
         * Applies configuration to all Jira automation rules.
         *
         * This is a convenience method for applying the same automation rules
         * to all build variants using Groovy closure syntax.
         *
         * @param configurationClosure The Groovy closure to apply to all automation configurations
         */
        fun automationCommon(
            @DelegatesTo(
                value = JiraAutomationConfig::class,
                strategy = Closure.DELEGATE_FIRST,
            )
            configurationClosure: Closure<in JiraAutomationConfig>,
        ) {
            common(automation) { target ->
                configureGroovy(configurationClosure, target)
            }
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
            variant: ApplicationVariant,
        ) {
            val variantName = input.buildVariant.name

            if (endpoints.isEmpty()) {
                throw GradleException(needToProvideEndpointsConfigMessage(variantName))
            }

            val automationConfig =
                automationConfigOrNull(variantName)
                    ?: throw GradleException(needToProvideAutomationConfigMessage(variantName))

            JiraTasksRegistrar.registerAutomationTask(
                project = project,
                automationConfig = automationConfig,
                params =
                    JiraAutomationTaskParams(
                        buildVariant = input.buildVariant,
                        issueNumberPattern = input.changelog.issueNumberPattern,
                        changelogFileProvider = input.changelog.fileProvider,
                        buildTagSnapshotProvider = input.output.buildTagSnapshotProvider,
                    ),
            )
        }
    }
