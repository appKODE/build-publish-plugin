package ru.kode.android.build.publish.plugin.firebase.remoteconfig

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor.KeyOccurrence
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.render.toSarif

class SarifRendererTest {
    private val audit =
        RemoteConfigAudit(
            currentTag = "v1.0.2",
            previousTag = "v1.0.1",
            templateVersion = "3",
            items =
                listOf(
                    RemoteConfigAudit.Item(
                        Severity.Fail,
                        Finding.MissingInRemote("missing", listOf(KeyOccurrence("missing", "app/Keys.kt", 4))),
                    ),
                    RemoteConfigAudit.Item(
                        Severity.Warn,
                        Finding.DefaultsMismatch("flag", "app/res/xml/defaults.xml", 7, "true", "false"),
                    ),
                    RemoteConfigAudit.Item(Severity.Warn, Finding.DefaultsMismatch("no_default", null, null, null, null)),
                    RemoteConfigAudit.Item(Severity.Info, Finding.UnusedInRemote("unused", null)),
                ),
            notes = emptyList(),
        )

    @Test
    fun `should render located findings as SARIF results`() {
        val run = Json.parseToJsonElement(audit.toSarif()).jsonObject.getValue("runs").jsonArray.single().jsonObject

        val results = run.getValue("results").jsonArray.map { it.jsonObject }
        assertEquals(
            listOf(
                "MissingInRemote error app/Keys.kt:4",
                "DefaultsMismatch warning app/res/xml/defaults.xml:7",
            ),
            results.map { it.summary() },
        )
        val rules = run.getValue("tool").jsonObject.getValue("driver").jsonObject.getValue("rules").jsonArray
        assertEquals(FindingType.entries.map { it.name }, rules.map { it.jsonObject.string("id") })
        assertEquals(
            "warning",
            rules.map { it.jsonObject }.first { it.string("id") == "PotentiallyStale" }
                .getValue("defaultConfiguration").jsonObject.string("level"),
        )
    }

    private fun JsonObject.summary(): String {
        val location = getValue("locations").jsonArray.single().jsonObject.getValue("physicalLocation").jsonObject
        val uri = location.getValue("artifactLocation").jsonObject.string("uri")
        val line = location.getValue("region").jsonObject.string("startLine")
        return "${string("ruleId")} ${string("level")} $uri:$line"
    }

    private fun JsonObject.string(name: String) = getValue(name).jsonPrimitive.content
}
