package ru.kode.android.build.publish.plugin.clickup.controller.entity

import kotlinx.serialization.Serializable

/**
 * A single ClickUp account baked into the ClickUp build-service parameters as JSON: its registry
 * [name], the API [token], and the [projects] (workspaces) declared on it.
 *
 * Mirrors the Jira instance model, but ClickUp has no self-hosted host, so an account carries only a
 * token (no base URL / credentials pair). One ClickUp service per build variant serves every declared
 * account.
 */
@Serializable
data class ClickUpAccountEntity(
    val name: String,
    val token: String,
    val projects: List<ClickUpProjectEntity>,
)
