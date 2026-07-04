package ru.kode.android.build.publish.plugin.jira.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.tasks.Nested
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy

/**
 * Configuration for Jira automation rules that can be applied during the build process.
 *
 * This interface defines the configuration for automating Jira workflows, such as:
 * - Transitioning issues to different statuses
 * - Adding labels based on build information
 * - Updating fix versions
 *
 * An automation rule targets one or more Jira projects — potentially on different Jira instances —
 * declared through the [projects] method. Each project carries its own project key, optional Jira
 * instance (via `instanceName`) and its own automation patterns; changelog issues are routed to a
 * project by their issue-key prefix.
 */
interface JiraAutomationConfig : CommonConfigMergeable<JiraAutomationConfig> {
    val name: String

    /**
     * Jira projects targeted by this automation rule.
     *
     * Configure projects through the [projects] method rather than accessing this directly.
     *
     * @see projects
     * @see JiraProjectsConfig
     */
    @get:Nested
    val projectsConfig: JiraProjectsConfig

    /**
     * Configures the Jira projects targeted by this automation rule.
     *
     * Each project (added via [JiraProjectsConfig.project]) points at its own project key and,
     * optionally, its own Jira instance (via `instanceName`) and per-project automation patterns.
     * Changelog issues are routed to a project by their issue-key prefix (e.g. `PROJ-123` -> `PROJ`).
     *
     * Example:
     * ```
     * projects {
     *     project("main")   { projectKey.set("APP"); fixVersionPattern.set("%s") }
     *     project("legacy") { projectKey.set("LEG"); instanceName.set("legacy"); targetStatusName.set("Done") }
     * }
     * ```
     *
     * @param action A configuration block applied to the [JiraProjectsConfig] container.
     *
     * @see JiraProjectsConfig
     */
    fun projects(action: Action<JiraProjectsConfig>) {
        action.execute(projectsConfig)
    }

    /**
     * Configures the Jira projects using a Groovy closure.
     *
     * @param configurationClosure The Groovy closure applied to the [JiraProjectsConfig] container.
     *
     * @see projects
     */
    fun projects(
        @DelegatesTo(value = JiraProjectsConfig::class, strategy = Closure.DELEGATE_FIRST)
        configurationClosure: Closure<in JiraProjectsConfig>,
    ) {
        projects { target -> configureGroovy(configurationClosure, target) }
    }

    /**
     * Shorthand for declaring a single Jira project without the surrounding `projects { }` block.
     *
     * Equivalent to `projects { project(name) { … } }`. Use the [projects] block instead when
     * targeting more than one project.
     *
     * @param name A unique identifier for the project (e.g. "main").
     * @param action A configuration block applied to the new [JiraProjectConfig] instance.
     */
    fun project(
        name: String,
        action: Action<JiraProjectConfig>,
    ) {
        projectsConfig.project(name, action)
    }

    /**
     * Shorthand for declaring a single Jira project with the given name using a Groovy closure.
     *
     * @param name A unique identifier for the project (e.g. "main").
     * @param configurationClosure The Groovy closure applied to the new [JiraProjectConfig].
     *
     * @see project
     */
    fun project(
        name: String,
        @DelegatesTo(value = JiraProjectConfig::class, strategy = Closure.DELEGATE_FIRST)
        configurationClosure: Closure<in JiraProjectConfig>,
    ) {
        project(name) { target -> configureGroovy(configurationClosure, target) }
    }

    /**
     * Shorthand for declaring the single Jira project of this automation rule without the surrounding
     * `projects { }` block and without naming it.
     *
     * Equivalent to `projects { project("default") { … } }`. Use the named [project] or the
     * [projects] block when targeting more than one project.
     *
     * @param action A configuration block applied to the new [JiraProjectConfig] instance.
     */
    fun project(action: Action<JiraProjectConfig>) {
        projectsConfig.project(DEFAULT_PROJECT_NAME, action)
    }

    /**
     * Shorthand for declaring the single unnamed Jira project of this automation rule using a Groovy
     * closure.
     *
     * @param configurationClosure The Groovy closure applied to the new [JiraProjectConfig].
     *
     * @see project
     */
    fun project(
        @DelegatesTo(value = JiraProjectConfig::class, strategy = Closure.DELEGATE_FIRST)
        configurationClosure: Closure<in JiraProjectConfig>,
    ) {
        project { target -> configureGroovy(configurationClosure, target) }
    }

    override fun inheritFrom(common: JiraAutomationConfig) {
        projectsConfig.inheritFrom(common.projectsConfig)
    }
}

private const val DEFAULT_PROJECT_NAME = "default"
