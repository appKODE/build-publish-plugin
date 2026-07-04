package ru.kode.android.build.publish.plugin.clickup.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable

/**
 * Configuration for resolving `CLOSES`/`FIXES` changelog references to ClickUp task names.
 *
 * Declaring this block opts a variant into resolution. Because ClickUp task ids share no common shape
 * with Jira keys, an optional [taskIdPattern] can scope which reference tokens this resolver attempts, so
 * it coexists cleanly with other providers (each returns `null` for references outside its scope). When
 * [taskIdPattern] is unset, every token is attempted and the resolver simply returns `null` for tasks
 * ClickUp does not know.
 */
interface ClickUpIssueResolutionConfig : CommonConfigMergeable<ClickUpIssueResolutionConfig> {
    val name: String

    /**
     * Optional Java regex a reference token must fully match to be treated as a ClickUp task id
     * (e.g. `"CU-[a-z0-9]+"`). When unset, every token is attempted.
     */
    @get:Input
    @get:Optional
    val taskIdPattern: Property<String>

    override fun inheritFrom(common: ClickUpIssueResolutionConfig) {
        taskIdPattern.convention(common.taskIdPattern)
    }
}
