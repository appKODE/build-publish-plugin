package ru.kode.android.build.publish.plugin.jira.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy

/**
 * Configuration for Jira automation rules applied during the build process (labels, fix versions,
 * status transitions).
 *
 * Projects are declared once in the shared registry (under `auth { … instance { projects { … } } }`).
 * An automation rule selects registry projects per instance via [targetInstance]; automation-level
 * patterns ([labelPattern], [fixVersionPattern], [targetStatusName]) apply to every selected project,
 * overridable per project inside a `targetInstance` block. Changelog issues are routed to a project by
 * their issue-key prefix.
 */
interface JiraAutomationConfig : CommonConfigMergeable<JiraAutomationConfig> {
    val name: String

    /**
     * The per-instance project selections this rule targets. Configure via [targetInstance].
     */
    @get:Nested
    val selectionsConfig: JiraInstanceSelectionsConfig

    /**
     * Default label pattern for every selected project (overridable per project). Formatted with
     * `buildVersion`, `buildNumber`, `buildVariant`.
     */
    @get:Input
    @get:Optional
    val labelPattern: Property<String>

    /**
     * Default fix-version pattern for every selected project (overridable per project).
     */
    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    /**
     * Default target status for every selected project (overridable per project).
     */
    @get:Input
    @get:Optional
    val targetStatusName: Property<String>

    /**
     * Selects the registry projects to automate on the instance named [instanceName].
     *
     * Example:
     * ```
     * targetInstance("default") { projectNames("app") }
     * targetInstance("legacy")  { project("leg") { targetStatusName.set("Done") } }
     * ```
     *
     * @param instanceName The auth instance to act on.
     * @param action A configuration block applied to the [JiraInstanceSelectionConfig].
     */
    fun targetInstance(
        instanceName: String,
        action: Action<JiraInstanceSelectionConfig>,
    ) {
        selectionsConfig.select(instanceName, action)
    }

    /**
     * Selects the registry projects to automate on the instance named [instanceName] using a Groovy
     * closure.
     *
     * @param instanceName The auth instance to act on.
     * @param configurationClosure The Groovy closure applied to the [JiraInstanceSelectionConfig].
     *
     * @see targetInstance
     */
    fun targetInstance(
        instanceName: String,
        @DelegatesTo(value = JiraInstanceSelectionConfig::class, strategy = Closure.DELEGATE_FIRST)
        configurationClosure: Closure<in JiraInstanceSelectionConfig>,
    ) {
        targetInstance(instanceName) { target -> configureGroovy(configurationClosure, target) }
    }

    override fun inheritFrom(common: JiraAutomationConfig) {
        selectionsConfig.inheritFrom(common.selectionsConfig)
        labelPattern.convention(common.labelPattern)
        fixVersionPattern.convention(common.fixVersionPattern)
        targetStatusName.convention(common.targetStatusName)
    }
}
