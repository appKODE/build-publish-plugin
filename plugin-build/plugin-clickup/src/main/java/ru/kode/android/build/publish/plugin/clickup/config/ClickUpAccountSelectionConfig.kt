package ru.kode.android.build.publish.plugin.clickup.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.inheritFrom
import ru.kode.android.build.publish.plugin.core.util.inheritNamedFrom
import javax.inject.Inject

/**
 * A two-level selection of registry projects on one ClickUp account.
 *
 * The selection's [name] is the account name; [projectNames] pick projects declared on that account in
 * the shared registry. Automation additionally allows per-project pattern overrides via [project]. This
 * same type is surfaced by two verbs: `targetAccount(name) { … }` in `automation` (which writes to the
 * account) and `fromAccount(name) { … }` in `issueResolution` (which reads from it).
 *
 * @see ClickUpProjectConfig For the per-project pattern override
 */
abstract class ClickUpAccountSelectionConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : Named,
        CommonConfigMergeable<ClickUpAccountSelectionConfig> {
        /**
         * Names of the registry projects (on this account) selected by this block.
         */
        @get:Input
        internal abstract val projectNames: SetProperty<String>

        /**
         * Per-project pattern overrides (automation only), keyed by registry project name.
         */
        internal val projectOverrides: NamedDomainObjectContainer<ClickUpProjectConfig> =
            objects.domainObjectContainer(ClickUpProjectConfig::class.java)

        /**
         * Selects one or more registry projects on this account.
         *
         * @param names Registry project names declared under this account.
         */
        fun projectNames(vararg names: String) {
            projectNames.addAll(names.toList())
        }

        /**
         * Selects a registry project and overrides its automation patterns. Selecting a project via this
         * method also adds it to [projectNames]. Ignored by `issueResolution`, which has no patterns.
         *
         * @param name A registry project name declared under this account.
         * @param action Configuration applied to the new [ClickUpProjectConfig] override.
         */
        fun project(
            name: String,
            action: Action<ClickUpProjectConfig>,
        ) {
            projectNames.add(name)
            projectOverrides.register(name, action)
        }

        /**
         * Selects a registry project and overrides its automation patterns using a Groovy closure.
         *
         * @param name A registry project name declared under this account.
         * @param configurationClosure The Groovy closure applied to the new [ClickUpProjectConfig].
         *
         * @see project
         */
        fun project(
            name: String,
            @DelegatesTo(value = ClickUpProjectConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in ClickUpProjectConfig>,
        ) {
            project(name) { target -> configureGroovy(configurationClosure, target) }
        }

        override fun inheritFrom(common: ClickUpAccountSelectionConfig) {
            projectNames.inheritFrom(common.projectNames)
            projectOverrides.inheritNamedFrom(common.projectOverrides)
        }
    }
