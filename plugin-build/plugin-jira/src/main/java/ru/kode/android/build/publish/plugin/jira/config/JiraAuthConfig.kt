package ru.kode.android.build.publish.plugin.jira.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.inheritNamedFrom
import javax.inject.Inject

/**
 * Container for one or more named Jira **instances**, each a base URL + credentials.
 *
 * Multiple instances can be declared for a single build — a project selects which one to use through
 * its `instanceName`. This mirrors the Telegram `bots { … bot("name") { … } }` model: instances are a
 * dedicated axis and are **not** tied to build variants (use `common { }` / `buildVariant(...) { }`
 * for the variant axis, and `instance("name") { }` for the instances within it).
 *
 * @see JiraInstanceConfig For the per-instance base URL / credentials / projects options
 */
abstract class JiraAuthConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : Named,
        CommonConfigMergeable<JiraAuthConfig> {
        /**
         * Internal container of Jira instances added via [instance].
         */
        internal val instances: NamedDomainObjectContainer<JiraInstanceConfig> =
            objects.domainObjectContainer(JiraInstanceConfig::class.java)

        /**
         * Declares a named Jira instance (base URL + credentials + its registry of projects).
         *
         * @param name A unique instance identifier (e.g. `"default"`, `"legacy"`) referenced by
         *             consumers via `targetInstance`/`fromInstance`.
         * @param action Configuration applied to the new [JiraInstanceConfig].
         */
        fun instance(
            name: String,
            action: Action<JiraInstanceConfig>,
        ) {
            instances.register(name, action)
        }

        /**
         * Declares a named Jira instance using a Groovy closure.
         *
         * @param name A unique instance identifier referenced by consumers via
         *             `targetInstance`/`fromInstance`.
         * @param configurationClosure The Groovy closure applied to the new [JiraInstanceConfig].
         *
         * @see instance
         */
        fun instance(
            name: String,
            @DelegatesTo(value = JiraInstanceConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in JiraInstanceConfig>,
        ) {
            instance(name) { target -> configureGroovy(configurationClosure, target) }
        }

        override fun inheritFrom(common: JiraAuthConfig) {
            instances.inheritNamedFrom(common.instances)
        }
    }
