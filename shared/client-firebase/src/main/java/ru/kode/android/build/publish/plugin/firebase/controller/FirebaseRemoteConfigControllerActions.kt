package ru.kode.android.build.publish.plugin.firebase.controller

import kotlinx.serialization.json.Json
import ru.kode.android.build.publish.plugin.core.git.GitCommandExecutor
import ru.kode.android.build.publish.plugin.firebase.config.KeySourceSpec
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigTemplate
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.AnalyzerInput
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.AuditRequest
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.InAppDefault
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.RemoteConfigAudit
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.TemplateChange
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.analyze
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.changedKeys
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor.KeyOccurrence
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.parseInAppDefaults
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.render.toMarkdown
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.render.toSarif
import ru.kode.android.gradle.commons.logger.PluginLogger
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

private val STRING_LITERAL = Regex(""""([A-Za-z0-9_.\-]+)"""")
private const val FIREBASE_MAX_VERSIONS = 300
private val REPORT_JSON = Json { prettyPrint = true }
private val FIREBASE_MAX_HISTORY = Duration.ofDays(90)

/**
 * Compares the keys declared in code at the current release tag (and the previous one) with the live
 * Remote Config template and the template versions published since the previous tag.
 */
fun FirebaseRemoteConfigController.audit(
    request: AuditRequest,
    git: GitCommandExecutor,
    logger: PluginLogger,
): RemoteConfigAudit {
    val current = request.snapshot.current
    val previous = request.snapshot.previous
    val template = getTemplate(request.projectId)
    val remoteKeys = template.allParameters.keys
    val notes = mutableListOf<String>()
    val findings =
        analyze(
            AnalyzerInput(
                currentKeys = git.extractKeys(current.commitSha, request.keySources),
                previousKeys = previous?.let { git.extractKeys(it.commitSha, request.keySources) },
                usedKeys = git.findUsedKeys(current.commitSha, request.usageSearchIncludes, remoteKeys),
                template = template,
                dynamicKeyPrefixes = request.dynamicKeyPrefixes,
                ignoreKeys = request.ignoreKeys,
                appVersion = request.appVersion,
                appBuild = request.appBuild,
                changes =
                    previous
                        ?.let { previousTag ->
                            val since = Instant.ofEpochMilli(git.commitTimeMs(previousTag.commitSha))
                            changesSince(request.projectId, since, notes, logger)
                        }
                        .orEmpty(),
                today = Instant.ofEpochMilli(git.commitTimeMs(current.commitSha)).atZone(ZoneOffset.UTC).toLocalDate(),
                lifetimes = request.flagLifetimes,
                inAppDefaults = git.readInAppDefaults(current.commitSha, request.inAppDefaultsIncludes, logger),
            ),
        )
    return RemoteConfigAudit(
        currentTag = current.name,
        previousTag = previous?.name,
        templateVersion = template.version?.versionNumber,
        items = findings.map { RemoteConfigAudit.Item(request.severities[it.type] ?: it.type.defaultSeverity, it) },
        notes = notes,
    )
}

/**
 * Writes `report.md`, `report.json` and `report.sarif` into [directory] and returns the Markdown report.
 */
fun RemoteConfigAudit.writeReports(directory: File): String {
    val markdown = toMarkdown()
    directory.mkdirs()
    directory.resolve("report.md").writeText(markdown)
    directory.resolve("report.json").writeText(REPORT_JSON.encodeToString(this))
    directory.resolve("report.sarif").writeText(toSarif())
    return markdown
}

private fun GitCommandExecutor.extractKeys(
    commitSha: String,
    keySources: List<KeySourceSpec>,
): List<KeyOccurrence> =
    keySources.flatMap { source ->
        buildList {
            forEachFileAt(commitSha, source.includes) { path, content ->
                addAll(source.extractor.extract(path, content))
            }
        }
    }

private fun GitCommandExecutor.readInAppDefaults(
    commitSha: String,
    includes: List<String>,
    logger: PluginLogger,
): List<InAppDefault> =
    buildList {
        forEachFileAt(commitSha, includes) { path, content ->
            runCatching { parseInAppDefaults(path, content) }
                .onSuccess(::addAll)
                .onFailure { logger.info("Skipping in-app defaults $path: it is not valid XML", it) }
        }
    }

private fun GitCommandExecutor.findUsedKeys(
    commitSha: String,
    includes: List<String>,
    remoteKeys: Set<String>,
): Set<String> =
    buildSet {
        forEachFileAt(commitSha, includes) { _, content ->
            STRING_LITERAL.findAll(content).map { it.groupValues[1] }.filterTo(this) { it in remoteKeys }
        }
    }

private fun FirebaseRemoteConfigController.changesSince(
    projectId: String,
    since: Instant,
    notes: MutableList<String>,
    logger: PluginLogger,
): List<TemplateChange> {
    val versions = listVersions(projectId, since)
    if (Duration.between(since, Instant.now()) > FIREBASE_MAX_HISTORY || versions.size >= FIREBASE_MAX_VERSIONS) {
        notes +=
            "Firebase keeps only the last $FIREBASE_MAX_VERSIONS template versions for 90 days, " +
            "history since $since may be incomplete."
    }
    val templates = mutableMapOf<Long, RemoteConfigTemplate?>()

    fun templateAt(versionNumber: Long) =
        templates.getOrPut(versionNumber) {
            runCatching { getTemplate(projectId, versionNumber.toString()) }
                .onFailure { logger.info("Remote Config template version $versionNumber is not available", it) }
                .getOrNull()
        }
    return versions.map { version ->
        val number = version.versionNumber.toLong()
        val new = templateAt(number)
        val old = templateAt(number - 1)
        TemplateChange(version, if (new != null && old != null) changedKeys(old, new) else emptyList())
    }
}
