package ru.kode.android.build.publish.plugin.jira.controller.entity

import kotlinx.serialization.Serializable

/**
 * A single Jira project baked into the [JiraInstanceEntity]: its registry [name] and globally-unique
 * [key]. Used by the service to route changelog issues by their key prefix and to resolve a registry
 * project name to its key at execution time.
 */
@Serializable
data class JiraProjectEntity(
    val name: String,
    val key: String,
)
