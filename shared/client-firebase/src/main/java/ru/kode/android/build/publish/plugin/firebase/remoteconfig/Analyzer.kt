package ru.kode.android.build.publish.plugin.firebase.remoteconfig

import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigParameter
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigTemplate
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigVersion
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.condition.ConditionEvaluator
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.condition.ConditionResult
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor.KeyOccurrence
import java.time.LocalDate

private val APP_VERSION_REFERENCE = Regex("""\bapp\.(version|build)\b""")

/**
 * A template version together with the keys it changed compared to the version before it.
 */
data class TemplateChange(
    val version: RemoteConfigVersion,
    val changedKeys: List<String>,
)

/**
 * Everything the [analyze] function needs; collected by the task, so the analysis stays a pure function.
 *
 * @property currentKeys Keys declared by the key sources at the current tag.
 * @property previousKeys Keys declared at the previous tag, `null` when there is no previous tag.
 * @property usedKeys Template keys found as string literals by the usage search.
 * @property ignoreKeys Regular expressions of keys excluded from every finding.
 * @property today Date the staleness of parameters is measured at (commit date of the current tag).
 * @property inAppDefaults Entries of the in-app defaults XML files at the current tag.
 * @property lifetimes Days a parameter of the metadata `type=` may live; types without an entry never go stale.
 */
data class AnalyzerInput(
    val currentKeys: List<KeyOccurrence>,
    val previousKeys: List<KeyOccurrence>?,
    val usedKeys: Set<String>,
    val template: RemoteConfigTemplate,
    val dynamicKeyPrefixes: Set<String>,
    val ignoreKeys: Set<String>,
    val appVersion: String?,
    val appBuild: String?,
    val changes: List<TemplateChange>,
    val today: LocalDate,
    val lifetimes: Map<String, Int>,
    val inAppDefaults: List<InAppDefault>,
)

fun analyze(input: AnalyzerInput): List<Finding> {
    val ignored = input.ignoreKeys.map(::Regex)

    fun relevant(key: String) = ignored.none { it.matches(key) }

    val current = input.currentKeys.filter { relevant(it.key) }.groupBy { it.key }
    val previous = input.previousKeys?.filter { relevant(it.key) }?.groupBy { it.key }
    val remote = input.template.allParameters.filterKeys(::relevant)
    val evaluator = ConditionEvaluator(input.appVersion, input.appBuild)
    val expressions = input.template.conditions.associate { it.name to it.expression }

    return buildList {
        if (previous != null) {
            (current.keys - previous.keys).sorted().forEach { add(Finding.AddedInRelease(it, current.getValue(it))) }
            (previous.keys - current.keys).sorted().forEach { add(Finding.RemovedInRelease(it, previous.getValue(it))) }
        }
        (current.keys - remote.keys).sorted().forEach { add(Finding.MissingInRemote(it, current.getValue(it))) }
        remote.keys
            .filter { key ->
                key !in current && key !in input.usedKeys && input.dynamicKeyPrefixes.none { key.startsWith(it) }
            }
            .sorted()
            .forEach { add(Finding.UnusedInRemote(it, remote.getValue(it).description)) }
        remote.toSortedMap().forEach { (key, parameter) ->
            parameter.conditionalValues.forEach { (condition, value) ->
                val expression = expressions[condition] ?: return@forEach
                if (!APP_VERSION_REFERENCE.containsMatchIn(expression)) return@forEach
                val result = evaluator.evaluate(expression)
                if (result != ConditionResult.NoMatch) {
                    add(Finding.TargetsRelease(key, condition, expression, result, value.display()))
                }
            }
        }
        addAll(staleParameters(remote, input.today, input.lifetimes))
        addAll(defaultsMismatches(remote, input.inAppDefaults))
        input.changes.forEach { change ->
            add(
                Finding.ChangedSinceRelease(
                    versionNumber = change.version.versionNumber,
                    updateTime = change.version.updateTime,
                    updateUser = change.version.updateUser?.let { it.email ?: it.name },
                    description = change.version.description,
                    changedKeys = change.changedKeys,
                ),
            )
        }
    }
}

private fun staleParameters(
    remote: Map<String, RemoteConfigParameter>,
    today: LocalDate,
    lifetimes: Map<String, Int>,
): List<Finding> =
    remote.toSortedMap().mapNotNull { (key, parameter) ->
        val metadata = parseMetadata(parameter.description)
        val reason = staleReason(metadata, today, lifetimes)
        reason?.let { Finding.PotentiallyStale(key, metadata.owner, metadata.type, it) }
    }

private fun defaultsMismatches(
    remote: Map<String, RemoteConfigParameter>,
    inAppDefaults: List<InAppDefault>,
): List<Finding> {
    val inAppByKey = inAppDefaults.associateBy { it.key }
    return remote.toSortedMap().mapNotNull { (key, parameter) ->
        val remoteDefault = parameter.defaultValue ?: return@mapNotNull null
        val inApp = inAppByKey[key]
        val differs = inApp != null && remoteDefault.value != null && remoteDefault.value != inApp.value
        if (differs || (inApp == null && remoteDefault.useInAppDefault == true)) {
            Finding.DefaultsMismatch(key, inApp?.path, inApp?.line, inApp?.value, remoteDefault.value)
        } else {
            null
        }
    }
}

private fun staleReason(
    metadata: ParameterMetadata,
    today: LocalDate,
    lifetimes: Map<String, Int>,
): String? {
    val lifetime = metadata.type?.let(lifetimes::get)
    return when {
        metadata.expires != null && metadata.expires < today -> "expired on ${metadata.expires}"
        metadata.created != null && lifetime != null && metadata.created.plusDays(lifetime.toLong()) < today ->
            "created on ${metadata.created}, older than the $lifetime days allowed for type=${metadata.type}"
        else -> null
    }
}

/**
 * Keys whose parameter (value, conditional values, description or type) differs between two templates,
 * plus keys whose conditions changed their expression.
 */
fun changedKeys(
    old: RemoteConfigTemplate,
    new: RemoteConfigTemplate,
): List<String> {
    val oldParameters = old.allParameters
    val newParameters = new.allParameters
    val oldExpressions = old.conditions.associate { it.name to it.expression }
    val newExpressions = new.conditions.associate { it.name to it.expression }
    val changedConditions =
        (oldExpressions.keys + newExpressions.keys).filter { oldExpressions[it] != newExpressions[it] }
    return (oldParameters.keys + newParameters.keys)
        .filter { key ->
            oldParameters[key] != newParameters[key] ||
                newParameters[key]?.conditionalValues?.keys.orEmpty().any { it in changedConditions }
        }
        .sorted()
}
