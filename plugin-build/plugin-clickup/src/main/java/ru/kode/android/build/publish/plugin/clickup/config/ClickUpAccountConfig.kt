package ru.kode.android.build.publish.plugin.clickup.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFile
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.inheritNamedFrom
import javax.inject.Inject

/**
 * A single ClickUp **account**: an API token, plus the shared registry of [projects] (workspaces) that
 * live on it.
 *
 * Declared via `auth { … { account("name") { … } } }`. ClickUp has no self-hosted host, so an account
 * carries only a token (no base URL). Projects nest under their account, so an account is implicit from
 * where a project is declared and consumers reference projects by name (`targetAccount`/`fromAccount`)
 * without repeating an `accountName`.
 *
 * @see ClickUpProjectDef For the per-project (workspace) registry entry
 */
abstract class ClickUpAccountConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : Named,
        CommonConfigMergeable<ClickUpAccountConfig> {
        /**
         * The file containing the ClickUp API token used for this account. The file should contain a
         * single line with the token.
         */
        @get:InputFile
        abstract val apiTokenFile: RegularFileProperty

        /**
         * Internal registry of projects (workspaces) that live on this account. Add projects via
         * [project].
         */
        internal val projects: NamedDomainObjectContainer<ClickUpProjectDef> =
            objects.domainObjectContainer(ClickUpProjectDef::class.java)

        /**
         * Registers a ClickUp project (workspace) on this account.
         *
         * @param name A unique-per-account identifier (e.g. "app"), referenced by consumers via
         *             `projectNames(...)`.
         * @param action Configuration applied to the new [ClickUpProjectDef].
         */
        fun project(
            name: String,
            action: Action<ClickUpProjectDef>,
        ) {
            projects.register(name, action)
        }

        /**
         * Registers a ClickUp project (workspace) on this account using a Groovy closure.
         *
         * @param name A unique-per-account identifier (e.g. "app").
         * @param configurationClosure The Groovy closure applied to the new [ClickUpProjectDef].
         *
         * @see project
         */
        fun project(
            name: String,
            @DelegatesTo(value = ClickUpProjectDef::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in ClickUpProjectDef>,
        ) {
            project(name) { target -> configureGroovy(configurationClosure, target) }
        }

        override fun inheritFrom(common: ClickUpAccountConfig) {
            apiTokenFile.convention(common.apiTokenFile)
            projects.inheritNamedFrom(common.projects)
        }
    }
