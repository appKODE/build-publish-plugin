package ru.kode.android.build.publish.plugin.firebase.config

import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.FindingType
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.Severity
import ru.kode.android.gradle.commons.util.CommonConfigMergeable
import javax.inject.Inject

/**
 * Configuration of the Firebase Remote Config audit (`checkFirebaseRemoteConfig<Variant>` task of the Firebase
 * plugin, `checkFirebaseRemoteConfig` task of the sender plugin).
 *
 * Collection properties set on a build variant replace the ones inherited from `remoteConfigCommon`.
 */
abstract class FirebaseRemoteConfigConfig : CommonConfigMergeable<FirebaseRemoteConfigConfig> {
    abstract val name: String

    @get:Inject
    protected abstract val objects: ObjectFactory

    /**
     * Service account JSON key with read access to Remote Config (`roles/cloudconfig.viewer`).
     */
    abstract val serviceCredentialsFile: RegularFileProperty

    /**
     * Firebase (Google Cloud) project id, e.g. `my-app-12345`.
     */
    abstract val projectId: Property<String>

    /**
     * Remote Config API host. Only needed for tests or a proxying gateway.
     */
    abstract val apiBaseUrl: Property<String>

    /**
     * Key sources declared with [keySource].
     */
    abstract val keySources: ListProperty<KeySourceSpec>

    /**
     * Globs declared with [usageSearch].
     */
    abstract val usageSearchIncludes: ListProperty<String>

    /**
     * Prefixes of keys built at runtime (e.g. `survey_`): template keys with these prefixes
     * are never reported as unused.
     */
    abstract val dynamicKeyPrefixes: SetProperty<String>

    /**
     * Regular expressions of keys excluded from the audit (e.g. `ios_.*` in a shared project).
     */
    abstract val ignoreKeys: SetProperty<String>

    /**
     * Severity overrides declared with [severity].
     */
    abstract val severities: MapProperty<FindingType, Severity>

    /**
     * Days a parameter with the description metadata `type=<name>` may live after its `created=` date before
     * it is reported as potentially stale. Defaults to 40 days for `release` and `experiment`; types without
     * an entry (`ops`, `permission`, `kill-switch`, `permanent`, …) never become stale by age.
     */
    abstract val flagLifetimes: MapProperty<String, Int>

    /**
     * Globs declared with [inAppDefaults].
     */
    abstract val inAppDefaultsIncludes: ListProperty<String>

    init {
        flagLifetimes.convention(DEFAULT_FLAG_TYPES.associateWith { DEFAULT_FLAG_LIFETIME_DAYS })
    }

    /**
     * Declares a named source of keys; see [RemoteConfigKeySource].
     */
    fun keySource(
        name: String,
        action: Action<RemoteConfigKeySource>,
    ) {
        val source = objects.newInstance(RemoteConfigKeySource::class.java, name)
        action.execute(source)
        keySources.add(
            source.includes.zip(source.extractor) { includes, extractor -> KeySourceSpec(name, includes, extractor) },
        )
    }

    /**
     * Globs of files searched for template keys used as string literals (`"key"`), so that keys read
     * outside the declared key sources (other platforms, native code) are not reported as unused.
     */
    fun usageSearch(vararg globs: String) {
        usageSearchIncludes.addAll(globs.toList())
    }

    /**
     * Globs of in-app defaults XML files (`<entry><key>…</key><value>…</value></entry>`, e.g.
     * `res/xml/remote_config_defaults.xml` of any module), compared with the Firebase defaults.
     */
    fun inAppDefaults(vararg globs: String) {
        inAppDefaultsIncludes.addAll(globs.toList())
    }

    fun severity(
        type: FindingType,
        severity: Severity,
    ) {
        severities.put(type, severity)
    }

    /**
     * String overload for Groovy scripts: `severity('MissingInRemote', 'Fail')`.
     */
    fun severity(
        type: String,
        severity: String,
    ) {
        severity(FindingType.valueOf(type), Severity.valueOf(severity))
    }

    override fun inheritFrom(common: FirebaseRemoteConfigConfig) {
        serviceCredentialsFile.convention(common.serviceCredentialsFile)
        projectId.convention(common.projectId)
        apiBaseUrl.convention(common.apiBaseUrl)
        keySources.convention(common.keySources)
        usageSearchIncludes.convention(common.usageSearchIncludes)
        dynamicKeyPrefixes.convention(common.dynamicKeyPrefixes)
        ignoreKeys.convention(common.ignoreKeys)
        severities.convention(common.severities)
        flagLifetimes.convention(common.flagLifetimes)
        inAppDefaultsIncludes.convention(common.inAppDefaultsIncludes)
    }
}

private const val DEFAULT_FLAG_LIFETIME_DAYS = 40
private val DEFAULT_FLAG_TYPES = listOf("release", "experiment")
