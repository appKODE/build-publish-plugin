package ru.kode.android.build.publish.plugin.jira.task.standalone

import org.gradle.api.GradleException
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import ru.kode.android.build.publish.plugin.jira.messages.unknownInstanceNameMessage
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService

/**
 * Resolves the [JiraService] a standalone task should use.
 *
 * When [instanceName] is set (via the `--instanceName` option) the matching service is selected from
 * [services], failing fast if no such auth configuration exists. Otherwise the [defaultService]
 * (the common / first configured instance) is used, preserving single-instance behavior.
 */
internal fun resolveStandaloneJiraService(
    instanceName: Property<String>,
    services: MapProperty<String, JiraService>,
    defaultService: Property<JiraService>,
): JiraService {
    val name = instanceName.orNull ?: return defaultService.get()
    val available = services.get()
    return available[name]
        ?: throw GradleException(unknownInstanceNameMessage(name, available.keys))
}
