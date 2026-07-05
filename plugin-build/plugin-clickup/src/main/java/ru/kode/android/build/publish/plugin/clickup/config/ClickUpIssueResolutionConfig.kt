package ru.kode.android.build.publish.plugin.clickup.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.tasks.Nested
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy

/**
 * Configuration for resolving `CLOSES`/`FIXES` changelog references to ClickUp task names.
 *
 * Declaring this block opts a variant into resolution. Projects (workspaces) are declared once in the
 * shared registry (under `auth { … account { project(…) { … } } }`); this rule selects registry projects
 * per account via [fromAccount]. A reference with a known custom-task-id prefix (`APP-123`) is routed to
 * its account+workspace; references without a known prefix are attempted against the selected accounts as
 * native ClickUp ids.
 */
interface ClickUpIssueResolutionConfig : CommonConfigMergeable<ClickUpIssueResolutionConfig> {
    val name: String

    /**
     * The per-account project selections whose tasks may appear in the changelog. Configure via
     * [fromAccount].
     */
    @get:Nested
    val selectionsConfig: ClickUpAccountSelectionsConfig

    /**
     * Selects the registry projects to resolve task names for on the account named [accountName].
     *
     * Example:
     * ```
     * fromAccount("main") { projectNames("app") }
     * fromAccount("secondary") { projectNames("lib") }
     * ```
     *
     * @param accountName The auth account to read from.
     * @param action A configuration block applied to the [ClickUpAccountSelectionConfig].
     */
    fun fromAccount(
        accountName: String,
        action: Action<ClickUpAccountSelectionConfig>,
    ) {
        selectionsConfig.select(accountName, action)
    }

    /**
     * Selects the registry projects to resolve task names for on the account named [accountName] using a
     * Groovy closure.
     *
     * @param accountName The auth account to read from.
     * @param configurationClosure The Groovy closure applied to the [ClickUpAccountSelectionConfig].
     *
     * @see fromAccount
     */
    fun fromAccount(
        accountName: String,
        @DelegatesTo(value = ClickUpAccountSelectionConfig::class, strategy = Closure.DELEGATE_FIRST)
        configurationClosure: Closure<in ClickUpAccountSelectionConfig>,
    ) {
        fromAccount(accountName) { target -> configureGroovy(configurationClosure, target) }
    }

    override fun inheritFrom(common: ClickUpIssueResolutionConfig) {
        selectionsConfig.inheritFrom(common.selectionsConfig)
    }
}
