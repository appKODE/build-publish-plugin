package ru.kode.android.build.publish.plugin.clickup.controller.entity

import kotlinx.serialization.Serializable

/**
 * A single ClickUp project baked into the [ClickUpAccountEntity]: its registry [name], the ClickUp
 * [workspaceName] (team) it maps to, and the globally-unique custom-task-id [taskIdPrefix]. Used by the
 * service to route changelog references by their prefix and to scope custom-task-id lookups to the
 * right workspace.
 */
@Serializable
data class ClickUpProjectEntity(
    val name: String,
    val workspaceName: String,
    val taskIdPrefix: String,
)
