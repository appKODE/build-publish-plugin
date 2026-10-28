package ru.kode.android.build.publish.plugin.firebase.remoteconfig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigCondition
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigParameter
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigParameterGroup
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigParameterValue
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigTemplate
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigVersion
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.condition.ConditionResult
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor.KeyOccurrence
import java.time.LocalDate

class AnalyzerTest {
    private fun keys(vararg keys: String) = keys.map { KeyOccurrence(it, "F.kt", 1) }

    private fun parameter(
        value: String = "true",
        conditional: Map<String, String> = emptyMap(),
    ) = RemoteConfigParameter(
        defaultValue = RemoteConfigParameterValue(value),
        conditionalValues = conditional.mapValues { RemoteConfigParameterValue(it.value) },
    )

    private val template =
        RemoteConfigTemplate(
            conditions =
                listOf(
                    RemoteConfigCondition("new_app", "app.version.>=(['2.0.0'])"),
                    RemoteConfigCondition("old_app", "app.version.<(['2.0.0'])"),
                    RemoteConfigCondition("ios", "device.os == 'ios'"),
                ),
            parameters =
                mapOf(
                    "kept" to parameter(conditional = mapOf("new_app" to "false", "old_app" to "x", "ios" to "y")),
                    "unused" to parameter(),
                    "survey_123" to parameter(),
                    "literal_only" to parameter(),
                    "ios_flag" to parameter(),
                ),
            parameterGroups = mapOf("group" to RemoteConfigParameterGroup(parameters = mapOf("grouped" to parameter()))),
        )

    private fun input(
        current: List<KeyOccurrence> = keys("kept", "grouped", "added", "missing_ios_free"),
        previous: List<KeyOccurrence>? = keys("kept", "grouped", "removed"),
        changes: List<TemplateChange> = emptyList(),
    ) = AnalyzerInput(
        currentKeys = current,
        previousKeys = previous,
        usedKeys = setOf("literal_only"),
        template = template,
        dynamicKeyPrefixes = setOf("survey_"),
        ignoreKeys = setOf("ios_.*", "missing_ios_free"),
        appVersion = "2.1.0",
        appBuild = "210",
        changes = changes,
        today = LocalDate.of(2026, 10, 1),
        lifetimes = mapOf("release" to 40),
        inAppDefaults = emptyList(),
    )

    @Test
    fun `should report every finding type`() {
        val version = RemoteConfigVersion("7", "2026-01-01T00:00:00Z", RemoteConfigVersion.User(email = "a@b.c"))

        val findings = analyze(input(changes = listOf(TemplateChange(version, listOf("kept")))))

        assertEquals(
            listOf(
                Finding.AddedInRelease("added", keys("added")),
                Finding.RemovedInRelease("removed", keys("removed")),
                Finding.MissingInRemote("added", keys("added")),
                Finding.UnusedInRemote("unused", null),
                Finding.TargetsRelease("kept", "new_app", "app.version.>=(['2.0.0'])", ConditionResult.Match, "false"),
                Finding.ChangedSinceRelease("7", "2026-01-01T00:00:00Z", "a@b.c", null, listOf("kept")),
            ),
            findings,
        )
    }

    @Test
    fun `should skip release diff without previous tag`() {
        val types = analyze(input(previous = null)).map { it.type }.toSet()

        assertEquals(setOf(FindingType.MissingInRemote, FindingType.UnusedInRemote, FindingType.TargetsRelease), types)
    }

    @Test
    fun `should detect changed keys including changed conditions`() {
        val changed =
            template.copy(
                conditions = template.conditions.map { if (it.name == "new_app") it.copy(expression = "true") else it },
                parameters = template.parameters + ("unused" to parameter(value = "false")) - "ios_flag",
            )

        assertEquals(listOf("ios_flag", "kept", "unused"), changedKeys(template, changed))
    }

    @Test
    fun `should report expired and outlived parameters as potentially stale`() {
        fun described(description: String) = RemoteConfigParameter(description = description)
        val described =
            template.copy(
                parameters =
                    mapOf(
                        "expired" to described("owner=@ivan expires=2026-09-30"),
                        "outlived" to described("type=release created=2026-08-01"),
                        "fresh" to described("type=release created=2026-09-01 expires=2026-12-01"),
                        "permanent" to described("type=permanent created=2020-01-01"),
                        "unknown_type" to described("created=2020-01-01"),
                    ),
                parameterGroups = emptyMap(),
            )
        val all = listOf("expired", "outlived", "fresh", "permanent", "unknown_type")

        val stale =
            analyze(input(current = keys(*all.toTypedArray()), previous = null).copy(template = described))
                .filterIsInstance<Finding.PotentiallyStale>()

        assertEquals(
            listOf(
                Finding.PotentiallyStale("expired", "@ivan", null, "expired on 2026-09-30"),
                Finding.PotentiallyStale(
                    "outlived",
                    null,
                    "release",
                    "created on 2026-08-01, older than the 40 days allowed for type=release",
                ),
            ),
            stale,
        )
    }

    @Test
    fun `should report differing defaults and missing in-app defaults`() {
        val withDefaults =
            template.copy(
                parameters =
                    mapOf(
                        "same" to parameter(value = "1"),
                        "differs" to parameter(value = "2"),
                        "in_app" to RemoteConfigParameter(RemoteConfigParameterValue(useInAppDefault = true)),
                        "no_in_app" to RemoteConfigParameter(RemoteConfigParameterValue(useInAppDefault = true)),
                        "remote_only" to parameter(value = "3"),
                    ),
                parameterGroups = emptyMap(),
            )
        val inApp =
            listOf(
                InAppDefault("same", "1", "defaults.xml", 3),
                InAppDefault("differs", "1", "defaults.xml", 7),
                InAppDefault("in_app", "x", "defaults.xml", 11),
            )

        val mismatches =
            analyze(input(previous = null).copy(template = withDefaults, inAppDefaults = inApp))
                .filterIsInstance<Finding.DefaultsMismatch>()

        assertEquals(
            listOf(
                Finding.DefaultsMismatch("differs", "defaults.xml", 7, "1", "2"),
                Finding.DefaultsMismatch("no_in_app", null, null, null, null),
            ),
            mismatches,
        )
    }
}
