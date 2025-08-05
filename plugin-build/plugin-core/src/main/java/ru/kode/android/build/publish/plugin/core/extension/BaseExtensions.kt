package ru.kode.android.build.publish.plugin.core.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.PolymorphicDomainObjectContainer
import  ru.kode.android.build.publish.plugin.core.util.createDefault

open class BaseExtension {

    protected fun <T> prepareDefault(
        container: PolymorphicDomainObjectContainer<T>,
        configurationAction: Action<in T>
    ) {
        container.createDefault(configurationAction)
    }

    protected fun <T> prepareDefault(
        container: NamedDomainObjectContainer<T>,
        configurationAction: Action<in T>
    ) {
        container.createDefault(configurationAction)
    }
}
