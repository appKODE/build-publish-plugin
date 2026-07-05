package ru.kode.android.build.publish.plugin.clickup.config

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
 * Configuration for ClickUp automation rules applied during the build process (tags, fix versions).
 *
 * Projects (workspaces) are declared once in the shared registry (under
 * `auth { … account { project(…) { … } } }`). An automation rule selects registry projects per account
 * via [targetAccount]; automation-level patterns ([fixVersionPattern], [fixVersionFieldName],
 * [tagPattern]) apply to every selected project, overridable per project inside a `targetAccount` block.
 */
interface ClickUpAutomationConfig : CommonConfigMergeable<ClickUpAutomationConfig> {
    val name: String

    /**
     * The per-account project selections this rule targets. Configure via [targetAccount].
     */
    @get:Nested
    val selectionsConfig: ClickUpAccountSelectionsConfig

    /**
     * Default fix-version pattern for every selected project (overridable per project). Formatted with
     * `buildVersion`, `buildNumber`, `buildVariant`.
     */
    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    /**
     * Default custom-field name used for fix versions (overridable per project).
     */
    @get:Input
    @get:Optional
    val fixVersionFieldName: Property<String>

    /**
     * Default tag pattern for every selected project (overridable per project). Formatted with
     * `buildVersion`, `buildNumber`, `buildVariant`.
     */
    @get:Input
    @get:Optional
    val tagPattern: Property<String>

    /**
     * Selects the registry projects to automate on the account named [accountName].
     *
     * Example:
     * ```
     * targetAccount("main") { projectNames("app") }
     * targetAccount("secondary") { project("lib") { tagPattern.set("v%1\$s") } }
     * ```
     *
     * @param accountName The auth account to act on.
     * @param action A configuration block applied to the [ClickUpAccountSelectionConfig].
     */
    fun targetAccount(
        accountName: String,
        action: Action<ClickUpAccountSelectionConfig>,
    ) {
        selectionsConfig.select(accountName, action)
    }

    /**
     * Selects the registry projects to automate on the account named [accountName] using a Groovy
     * closure.
     *
     * @param accountName The auth account to act on.
     * @param configurationClosure The Groovy closure applied to the [ClickUpAccountSelectionConfig].
     *
     * @see targetAccount
     */
    fun targetAccount(
        accountName: String,
        @DelegatesTo(value = ClickUpAccountSelectionConfig::class, strategy = Closure.DELEGATE_FIRST)
        configurationClosure: Closure<in ClickUpAccountSelectionConfig>,
    ) {
        targetAccount(accountName) { target -> configureGroovy(configurationClosure, target) }
    }

    override fun inheritFrom(common: ClickUpAutomationConfig) {
        selectionsConfig.inheritFrom(common.selectionsConfig)
        fixVersionPattern.convention(common.fixVersionPattern)
        fixVersionFieldName.convention(common.fixVersionFieldName)
        tagPattern.convention(common.tagPattern)
    }
}
