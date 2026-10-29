package ru.kode.android.build.publish.plugin.firebase.config

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor.RegexKeyExtractor
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor.RemoteConfigKeyExtractor
import java.io.Serializable
import javax.inject.Inject

/**
 * Where Remote Config keys are declared in the repository and how to extract them.
 *
 * ```kotlin
 * keySource("toggles") {
 *     include("app/src/main/kotlin/**/AppFeature.kt")
 *     extractor.set(regex("""AppFeature\(\s*"([^"]+)""""))
 * }
 * ```
 */
abstract class RemoteConfigKeySource
    @Inject
    constructor(val name: String) {
        /**
         * Repository-relative `java.nio` globs of the files to scan.
         */
        abstract val includes: ListProperty<String>

        abstract val extractor: Property<RemoteConfigKeyExtractor>

        fun include(vararg globs: String) {
            includes.addAll(globs.toList())
        }

        /**
         * Extractor returning the first capture group of every match of [pattern].
         */
        fun regex(pattern: String): RemoteConfigKeyExtractor = RegexKeyExtractor(pattern)

        /**
         * Extractor for Firebase in-app defaults files (`res/xml/remote_config_defaults.xml`).
         */
        fun defaultsXml(): RemoteConfigKeyExtractor = RegexKeyExtractor.DEFAULTS_XML
    }

/**
 * Resolved [RemoteConfigKeySource], passed to the audit task as an input.
 */
data class KeySourceSpec(
    val name: String,
    val includes: List<String>,
    val extractor: RemoteConfigKeyExtractor,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
