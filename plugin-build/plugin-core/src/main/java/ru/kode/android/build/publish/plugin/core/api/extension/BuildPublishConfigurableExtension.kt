package ru.kode.android.build.publish.plugin.core.api.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import ru.kode.android.build.publish.plugin.core.api.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.util.buildType
import ru.kode.android.build.publish.plugin.core.util.common

open class BuildPublishConfigurableExtension {
    protected fun <T> common(
        container: NamedDomainObjectContainer<T>,
        configurationAction: Action<in T>,
    ) {
        container.common(configurationAction)
    }

    protected fun <T> common(
        container: BaseDomainContainer<T>,
        configurationAction: Action<in T>,
    ) {
        container.common(configurationAction)
    }

    protected fun <T> buildType(
        buildType: String,
        container: NamedDomainObjectContainer<T>,
        configurationAction: Action<in T>,
    ) {
        container.buildType(buildType, configurationAction)
    }

    protected fun <T> buildType(
        buildType: String,
        container: BaseDomainContainer<T>,
        configurationAction: Action<in T>,
    ) {
        container.buildType(buildType, configurationAction)
    }

    open fun configure(
        project: Project,
        input: ExtensionInput,
    ) = Unit
}
