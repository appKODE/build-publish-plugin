package ru.kode.android.build.publish.plugin.jira.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.tasks.Nested
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy

/**
 * Configuration for resolving `CLOSES`/`FIXES` changelog references to Jira issue titles.
 *
 * Declaring this block opts a variant into resolution. Projects are declared once in the shared registry
 * (under `auth { … instance { project(…) { … } } }`); this rule selects registry projects per instance
 * via [fromInstance]. A bare issue number (`3458`) is resolved against the single selected project; with
 * several projects selected, use prefixed keys (`APP-3458`).
 */
interface JiraIssueResolutionConfig : CommonConfigMergeable<JiraIssueResolutionConfig> {
    val name: String

    /**
     * The per-instance project selections whose issues may appear in the changelog. Configure via
     * [fromInstance].
     */
    @get:Nested
    val selectionsConfig: JiraInstanceSelectionsConfig

    /**
     * Selects the registry projects to resolve titles for on the instance named [instanceName].
     *
     * Example:
     * ```
     * fromInstance("default") { projectNames("app") }
     * fromInstance("legacy")  { projectNames("leg") }
     * ```
     *
     * @param instanceName The auth instance to read from.
     * @param action A configuration block applied to the [JiraInstanceSelectionConfig].
     */
    fun fromInstance(
        instanceName: String,
        action: Action<JiraInstanceSelectionConfig>,
    ) {
        selectionsConfig.select(instanceName, action)
    }

    /**
     * Selects the registry projects to resolve titles for on the instance named [instanceName] using a
     * Groovy closure.
     *
     * @param instanceName The auth instance to read from.
     * @param configurationClosure The Groovy closure applied to the [JiraInstanceSelectionConfig].
     *
     * @see fromInstance
     */
    fun fromInstance(
        instanceName: String,
        @DelegatesTo(value = JiraInstanceSelectionConfig::class, strategy = Closure.DELEGATE_FIRST)
        configurationClosure: Closure<in JiraInstanceSelectionConfig>,
    ) {
        fromInstance(instanceName) { target -> configureGroovy(configurationClosure, target) }
    }

    override fun inheritFrom(common: JiraIssueResolutionConfig) {
        selectionsConfig.inheritFrom(common.selectionsConfig)
    }
}
