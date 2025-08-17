package ru.kode.android.build.publish.plugin.jira.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.jira.service.network.JiraNetworkService

/**
 * Extension for configuring Jira integration in the build script.
 *
 * This extension provides access to Jira network services that can be used by tasks
 * to interact with Jira's REST API.
 *
 * @property networkServices
 */
abstract class JiraServiceExtension(
    /**
     * A provider of named [JiraNetworkService] instances that can be used
     * to make authenticated requests to the Jira API. The map keys are
     * typically variant names, with a special "common" key for shared services.
     */
    val networkServices: Provider<Map<String, Provider<JiraNetworkService>>>,
)
