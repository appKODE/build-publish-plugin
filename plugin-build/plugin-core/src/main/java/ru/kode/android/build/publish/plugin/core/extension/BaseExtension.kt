package ru.kode.android.build.publish.plugin.core.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.container.BaseDomainContainer
import ru.kode.android.build.publish.plugin.core.util.buildType
import ru.kode.android.build.publish.plugin.core.util.common

open class BaseExtension {

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
}
