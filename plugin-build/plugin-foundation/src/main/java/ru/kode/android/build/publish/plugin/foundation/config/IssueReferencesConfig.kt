package ru.kode.android.build.publish.plugin.foundation.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.inheritNamedFrom
import javax.inject.Inject

/**
 * Container of issue-reference markers used to derive changelog entries from commit messages.
 *
 * Declared inside `changelog { … { issueReferences { … } } }`. Each reference is added via the
 * [issueReference] method and names a commit marker (`CLOSES`/`FIXES`) plus the pattern extracting its
 * issue token. When a provider plugin (Jira, ClickUp, …) is applied, every extracted token is resolved
 * to its tracker title automatically.
 *
 * `numberPattern` is optional and defaults to [IssueReferenceConfig.DEFAULT_NUMBER_PATTERN] (the
 * standard bare-number-or-prefixed-key pattern), so usually only [IssueReferenceConfig.key] is set.
 *
 * Example:
 * ```
 * issueReferences {
 *     issueReference("closes") { key.set("CLOSES") }
 *     issueReference("fixes")  { key.set("FIXES") }
 *     // override the pattern only when the default does not fit:
 *     issueReference("ref")    { key.set("REF"); numberPattern.set("REF-\\d+") }
 * }
 * ```
 *
 * @see IssueReferenceConfig
 */
abstract class IssueReferencesConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : CommonConfigMergeable<IssueReferencesConfig> {
        /**
         * Internal container of the configured issue references. Access is restricted to the plugin
         * implementation; use [issueReference] to add references.
         */
        internal val references: NamedDomainObjectContainer<IssueReferenceConfig> =
            objects.domainObjectContainer(IssueReferenceConfig::class.java)

        /**
         * Configures a new issue reference with the given name and settings.
         *
         * @param name A unique identifier for the reference (e.g. "closes", "fixes"). Used only to
         *             distinguish references within the build script.
         * @param action A configuration block applied to the new [IssueReferenceConfig] instance.
         *
         * @see IssueReferenceConfig For available per-reference options
         */
        fun issueReference(
            name: String,
            action: Action<IssueReferenceConfig>,
        ) {
            references.register(name) { reference ->
                reference.numberPattern.convention(IssueReferenceConfig.DEFAULT_NUMBER_PATTERN)
                action.execute(reference)
            }
        }

        /**
         * Configures a new issue reference with the given name using a Groovy closure.
         *
         * @param name A unique identifier for the reference (e.g. "closes").
         * @param configurationClosure The Groovy closure applied to the new [IssueReferenceConfig].
         *
         * @see issueReference
         */
        fun issueReference(
            name: String,
            @DelegatesTo(value = IssueReferenceConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in IssueReferenceConfig>,
        ) {
            issueReference(name) { target -> configureGroovy(configurationClosure, target) }
        }

        override fun inheritFrom(common: IssueReferencesConfig) {
            references.inheritNamedFrom(common.references)
        }
    }
