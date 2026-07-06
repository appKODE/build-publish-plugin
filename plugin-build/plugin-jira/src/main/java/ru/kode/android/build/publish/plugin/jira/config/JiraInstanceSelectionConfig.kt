package ru.kode.android.build.publish.plugin.jira.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.inheritFrom
import ru.kode.android.build.publish.plugin.core.util.inheritNamedFrom
import javax.inject.Inject

/**
 * A two-level selection of registry projects on one Jira instance — the analog of Telegram's
 * `destinationBot { botName; chatNames }`.
 *
 * The selection's [name] is the instance name; [projectNames] pick projects declared on that instance
 * in the shared registry. Automation additionally allows per-project pattern overrides via
 * [project]. This same type is surfaced by two verbs: `targetInstance(name) { … }` in `automation`
 * (which writes to the instance) and `fromInstance(name) { … }` in `issueResolution` (which reads from
 * it).
 *
 * @see JiraProjectConfig For the per-project pattern override
 */
abstract class JiraInstanceSelectionConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : Named,
        CommonConfigMergeable<JiraInstanceSelectionConfig> {
        /**
         * Names of the registry projects (on this instance) selected by this block.
         */
        @get:org.gradle.api.tasks.Input
        internal abstract val projectNames: SetProperty<String>

        /**
         * Per-project pattern overrides (automation only), keyed by registry project name.
         */
        internal val projectOverrides: NamedDomainObjectContainer<JiraProjectConfig> =
            objects.domainObjectContainer(JiraProjectConfig::class.java)

        /**
         * Selects one or more registry projects on this instance.
         *
         * @param names Registry project names declared under this instance.
         */
        fun projectNames(vararg names: String) {
            projectNames.addAll(names.toList())
        }

        /**
         * Selects a registry project and overrides its automation patterns. Selecting a project via
         * this method also adds it to [projectNames]. Ignored by `issueResolution`, which has no
         * patterns.
         *
         * @param name A registry project name declared under this instance.
         * @param action Configuration applied to the new [JiraProjectConfig] override.
         */
        fun project(
            name: String,
            action: Action<JiraProjectConfig>,
        ) {
            projectNames.add(name)
            projectOverrides.register(name, action)
        }

        /**
         * Selects a registry project and overrides its automation patterns using a Groovy closure.
         *
         * @param name A registry project name declared under this instance.
         * @param configurationClosure The Groovy closure applied to the new [JiraProjectConfig].
         *
         * @see project
         */
        fun project(
            name: String,
            @DelegatesTo(value = JiraProjectConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in JiraProjectConfig>,
        ) {
            project(name) { target -> configureGroovy(configurationClosure, target) }
        }

        override fun inheritFrom(common: JiraInstanceSelectionConfig) {
            projectNames.inheritFrom(common.projectNames)
            projectOverrides.inheritNamedFrom(common.projectOverrides)
        }
    }
