package ru.kode.android.build.publish.plugin.clickup.config

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
 * Container for one or more named ClickUp **accounts**, each an API token plus its registry of projects
 * (workspaces).
 *
 * Multiple accounts can be declared for a single build — consumers select which one to use through
 * `targetAccount`/`fromAccount`. Accounts are a dedicated axis and are **not** tied to build variants
 * (use `common { }` / `buildVariant(...) { }` for the variant axis, and `account("name") { }` for the
 * accounts within it). ClickUp has no self-hosted host, so an account carries only a token — there is no
 * per-instance base URL.
 *
 * @see ClickUpAccountConfig For the per-account token / projects options
 */
abstract class ClickUpAuthConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : Named,
        CommonConfigMergeable<ClickUpAuthConfig> {
        /**
         * Internal container of ClickUp accounts added via [account].
         */
        internal val accounts: NamedDomainObjectContainer<ClickUpAccountConfig> =
            objects.domainObjectContainer(ClickUpAccountConfig::class.java)

        /**
         * Declares a named ClickUp account (API token + its registry of projects).
         *
         * @param name A unique account identifier (e.g. `"main"`, `"secondary"`) referenced by consumers
         *             via `targetAccount`/`fromAccount`.
         * @param action Configuration applied to the new [ClickUpAccountConfig].
         */
        fun account(
            name: String,
            action: Action<ClickUpAccountConfig>,
        ) {
            accounts.register(name, action)
        }

        /**
         * Declares a named ClickUp account using a Groovy closure.
         *
         * @param name A unique account identifier referenced by consumers via
         *             `targetAccount`/`fromAccount`.
         * @param configurationClosure The Groovy closure applied to the new [ClickUpAccountConfig].
         *
         * @see account
         */
        fun account(
            name: String,
            @DelegatesTo(value = ClickUpAccountConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in ClickUpAccountConfig>,
        ) {
            account(name) { target -> configureGroovy(configurationClosure, target) }
        }

        override fun inheritFrom(common: ClickUpAuthConfig) {
            accounts.inheritNamedFrom(common.accounts)
        }
    }
