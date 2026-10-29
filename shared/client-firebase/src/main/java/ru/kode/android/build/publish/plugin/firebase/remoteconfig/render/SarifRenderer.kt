package ru.kode.android.build.publish.plugin.firebase.remoteconfig.render

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.Finding
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.FindingType
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.RemoteConfigAudit
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.Severity
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor.KeyOccurrence

private const val SARIF_SCHEMA = "https://json.schemastore.org/sarif-2.1.0.json"
private const val SARIF_VERSION = "2.1.0"
private const val TOOL_NAME = "build-publish-novo Remote Config audit"
private const val TOOL_URI = "https://github.com/appKODE/build-publish-plugin"
private val SARIF_JSON = Json { prettyPrint = true }

/**
 * Renders the audit as SARIF 2.1.0 (e.g. for `github/codeql-action/upload-sarif`). Only findings that point
 * at a file of the current tree become results; paths are relative to the repository root.
 */
fun RemoteConfigAudit.toSarif(): String {
    val rules =
        FindingType.entries.map { type ->
            Rule(type.name, Text(type.title()), Configuration(type.defaultSeverity.level()))
        }
    val results =
        items.mapNotNull { item ->
            val locations = item.finding.locations().ifEmpty { return@mapNotNull null }
            Result(item.finding.type.name, item.severity.level(), Text(item.finding.line()), locations)
        }
    val run = Run(Tool(Driver(TOOL_NAME, TOOL_URI, rules)), results)
    return SARIF_JSON.encodeToString(Log(SARIF_SCHEMA, SARIF_VERSION, listOf(run)))
}

private fun Severity.level() =
    when (this) {
        Severity.Info -> "note"
        Severity.Warn -> "warning"
        Severity.Fail -> "error"
    }

private fun Finding.locations(): List<Location> =
    when (this) {
        is Finding.AddedInRelease -> occurrences.map(KeyOccurrence::location)
        is Finding.MissingInRemote -> occurrences.map(KeyOccurrence::location)
        is Finding.DefaultsMismatch -> path?.let { listOf(location(it, line ?: 1)) }.orEmpty()
        is Finding.RemovedInRelease,
        is Finding.UnusedInRemote,
        is Finding.TargetsRelease,
        is Finding.ChangedSinceRelease,
        is Finding.PotentiallyStale,
        -> emptyList()
    }

private fun KeyOccurrence.location() = location(path, line)

private fun location(
    path: String,
    line: Int,
) = Location(PhysicalLocation(ArtifactLocation(path), Region(line)))

@Serializable
private data class Log(
    @SerialName("\$schema") val schema: String,
    val version: String,
    val runs: List<Run>,
)

@Serializable
private data class Run(val tool: Tool, val results: List<Result>)

@Serializable
private data class Tool(val driver: Driver)

@Serializable
private data class Driver(val name: String, val informationUri: String, val rules: List<Rule>)

@Serializable
private data class Rule(val id: String, val shortDescription: Text, val defaultConfiguration: Configuration)

@Serializable
private data class Configuration(val level: String)

@Serializable
private data class Text(val text: String)

@Serializable
private data class Result(val ruleId: String, val level: String, val message: Text, val locations: List<Location>)

@Serializable
private data class Location(val physicalLocation: PhysicalLocation)

@Serializable
private data class PhysicalLocation(val artifactLocation: ArtifactLocation, val region: Region)

@Serializable
private data class ArtifactLocation(val uri: String)

@Serializable
private data class Region(val startLine: Int)
