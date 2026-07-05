package ru.kode.android.build.publish.plugin.clickup.controller.mappers

import kotlinx.serialization.json.Json
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpAccountEntity

/** Encodes a [ClickUpAccountEntity] to the JSON baked into the ClickUp build-service parameters. */
fun ClickUpAccountEntity.toJson(): String = Json.encodeToString(this)

/** Decodes a [ClickUpAccountEntity] from the JSON baked into the ClickUp build-service parameters. */
fun clickUpAccountFromJson(json: String): ClickUpAccountEntity = Json.decodeFromString(json)
