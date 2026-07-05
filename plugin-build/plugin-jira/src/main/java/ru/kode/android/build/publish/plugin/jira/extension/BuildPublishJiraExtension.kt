package ru.kode.android.build.publish.plugin.jira.extension

import com.android.build.api.variant.ApplicationVariant
import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.entity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.getByNameOrCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrNullableCommon
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import ru.kode.android.build.publish.plugin.jira.config.JiraAuthConfig
import ru.kode.android.build.publish.plugin.jira.config.JiraAutomationConfig
import ru.kode.android.build.publish.plugin.jira.config.JiraIssueResolutionConfig
import ru.kode.android.build.publish.plugin.jira.issue.JiraIssueResolver
import ru.kode.android.build.publish.plugin.jira.messages.duplicateProjectKeyMessage
import ru.kode.android.build.publish.plugin.jira.messages.needToProvideAuthConfigMessage
import ru.kode.android.build.publish.plugin.jira.messages.needToProvideAutomationConfigMessage
import ru.kode.android.build.publish.plugin.jira.messages.unknownInstanceNameMessage
import ru.kode.android.build.publish.plugin.jira.messages.unknownProjectNameMessage
import ru.kode.android.build.publish.plugin.jira.service.JiraServiceExtension
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService
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
         * Container for Jira issue-resolution configurations. Each enables resolving `CLOSES`/`FIXES`
         * changelog references to Jira issue titles for a build variant (or `common`).
         */
        internal val issueResolution: NamedDomainObjectContainer<JiraIssueResolutionConfig> =
            objectFactory.domainObjectContainer(JiraIssueResolutionConfig::class.java)

        private val issueResolutionConfigOrNull: (buildName: String) -> JiraIssueResolutionConfig? =
            { buildName -> issueResolution.getByNameOrNullableCommon(buildName) }

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
            configurationAction: Action<BuildPublishDomainObjectContainer<JiraAuthConfig>>,
        ) {
            val container = BuildPublishDomainObjectContainer(auth)
            configurationAction.execute(container)
        }

        /**
         * Configures Jira authentication settings using a Groovy closure.
         *
         * @param configurationClosure The Groovy closure to apply to the auth container
         * @see JiraAuthConfig For available configuration options
         */
        fun auth(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationClosure: Closure<in BuildPublishDomainObjectContainer<JiraAuthConfig>>,
        ) {
            val container = BuildPublishDomainObjectContainer(auth)
            configureGroovy(configurationClosure, container)
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
         * Applies configuration to all Jira authentication settings.
         *
         * @param configurationAction The configuration action to apply to all auth configurations
         */
        fun authCommon(configurationAction: Action<JiraAuthConfig>) {
            common(auth, configurationAction)
        }

        /**
         * Applies configuration to all Jira authentication settings using a Groovy closure.
         *
         * @param configurationClosure The Groovy closure to configure all auth configurations
         */
        fun authCommon(
            @DelegatesTo(
                value = JiraAuthConfig::class,
                strategy = Closure.DELEGATE_FIRST,
            )
            configurationClosure: Closure<in JiraAuthConfig>,
        ) {
            common(auth) { target ->
                configureGroovy(configurationClosure, target)
            }
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
         * Configures Jira issue-resolution (changelog title fetching).
         *
         * @param configurationAction The action applied to the issue-resolution container
         * @see JiraIssueResolutionConfig For available configuration options
         */
        fun issueResolution(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationAction: Action<BuildPublishDomainObjectContainer<JiraIssueResolutionConfig>>,
        ) {
            val container = BuildPublishDomainObjectContainer(issueResolution)
            configurationAction.execute(container)
        }

        /**
         * Configures Jira issue-resolution using a Groovy closure.
         *
         * @param configurationClosure The Groovy closure applied to the issue-resolution container
         */
        fun issueResolution(
            @DelegatesTo(BuildPublishDomainObjectContainer::class)
            configurationClosure: Closure<in BuildPublishDomainObjectContainer<JiraIssueResolutionConfig>>,
        ) {
            val container = BuildPublishDomainObjectContainer(issueResolution)
            configureGroovy(configurationClosure, container)
        }

        /**
         * Applies the same issue-resolution configuration to all build variants.
         *
         * @param configurationAction The action applied to all issue-resolution configurations
         */
        fun issueResolutionCommon(configurationAction: Action<JiraIssueResolutionConfig>) {
            common(issueResolution, configurationAction)
        }

        /**
         * Applies the same issue-resolution configuration to all build variants using a Groovy closure.
         *
         * @param configurationClosure The Groovy closure applied to all issue-resolution configurations
         */
        fun issueResolutionCommon(
            @DelegatesTo(
                value = JiraIssueResolutionConfig::class,
                strategy = Closure.DELEGATE_FIRST,
            )
            configurationClosure: Closure<in JiraIssueResolutionConfig>,
        ) {
            common(issueResolution) { target ->
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

            if (auth.isEmpty()) {
                throw GradleException(needToProvideAuthConfigMessage(variantName))
            }

            val authConfig = authConfig(variantName)
            validateProjectKeysUnique(authConfig)

            val automationConfig = automationConfigOrNull(variantName)
            val resolutionConfig = issueResolutionConfigOrNull(variantName)

            if (automationConfig == null && resolutionConfig == null) {
                throw GradleException(needToProvideAutomationConfigMessage(variantName))
            }

            val service =
                project.extensions
                    .getByType(JiraServiceExtension::class.java)
                    .services
                    .get()
                    .getByNameOrCommon<JiraService>(variantName)

            if (automationConfig != null) {
                JiraTasksRegistrar.registerAutomationTask(
                    project = project,
                    authConfig = authConfig,
                    automationConfig = automationConfig,
                    service = service,
                    params =
                        JiraAutomationTaskParams(
                            buildVariant = input.buildVariant,
                            issuePatterns =
                                input.changelog.issueSources.map { sources ->
                                    sources.map { it.numberPattern }
                                },
                            changelogFileProvider = input.changelog.fileProvider,
                            buildTagSnapshotProvider = input.output.buildTagSnapshotProvider,
                        ),
                )
            }

            if (resolutionConfig != null) {
                injectIssueResolver(input, authConfig, resolutionConfig, service)
            }
        }

        /**
         * Fails fast when two registry projects (even on different instances) declare the same key —
         * issues are routed to a project by their key prefix, so keys must be globally unique.
         */
        private fun validateProjectKeysUnique(authConfig: JiraAuthConfig) {
            val duplicate =
                authConfig.instances
                    .flatMap { instance -> instance.projects.mapNotNull { it.projectKey.orNull?.uppercase() } }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .firstOrNull { (_, count) -> count > 1 }
                    ?.key
            if (duplicate != null) throw GradleException(duplicateProjectKeyMessage(duplicate))
        }

        /**
         * Builds a [JiraIssueResolver] from the issue-resolution selections and appends it to
         * the foundation changelog task's resolver list, declaring the single Jira [service] it uses.
         * This is the only cross-plugin touch and flows plugin-jira -> foundation task.
         */
        private fun injectIssueResolver(
            input: ExtensionInput,
            authConfig: JiraAuthConfig,
            resolutionConfig: JiraIssueResolutionConfig,
            service: Provider<JiraService>,
        ) {
            val selectedProjects =
                resolutionConfig.selectionsConfig.selections.flatMap { selection ->
                    val instanceName = selection.name
                    val instanceConfig =
                        authConfig.instances.findByName(instanceName)
                            ?: throw GradleException(
                                unknownInstanceNameMessage(instanceName, authConfig.instances.names),
                            )
                    selection.projectNames.get().map { projectName ->
                        val projectKey =
                            instanceConfig.projects.findByName(projectName)?.projectKey?.orNull
                                ?: throw GradleException(
                                    unknownProjectNameMessage(projectName, instanceName, instanceConfig.projects.names),
                                )
                        projectKey to instanceName
                    }
                }
            val instanceByPrefix = selectedProjects.associate { (key, instance) -> key.uppercase() to instance }
            val soleProjectKey = selectedProjects.map { it.first }.distinct().singleOrNull()

            val resolver = JiraIssueResolver(service, instanceByPrefix, soleProjectKey)
            input.changelog.fileProvider.configure { task ->
                task.issueResolvers.add(resolver)
                task.usesService(service)
            }
        }
    }
