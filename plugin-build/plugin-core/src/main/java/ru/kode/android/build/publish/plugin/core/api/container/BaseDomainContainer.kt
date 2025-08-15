package ru.kode.android.build.publish.plugin.core.api.container

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.UnknownDomainObjectException
import ru.kode.android.build.publish.plugin.core.util.COMMON_CONTAINER_NAME

class BaseDomainContainer<T>(
    internal val namedContainer: NamedDomainObjectContainer<T>,
) {
    @Throws(InvalidUserDataException::class)
    fun common(configurationAction: Action<in T>): NamedDomainObjectProvider<T> {
        return namedContainer.register(COMMON_CONTAINER_NAME, configurationAction)
    }

    @Throws(InvalidUserDataException::class)
    fun buildType(
        buildType: String,
        configurationAction: Action<in T>,
    ): NamedDomainObjectProvider<T> {
        return namedContainer.register(buildType, configurationAction)
    }

    fun findByName(name: String): T? {
        return namedContainer.findByName(name)
    }

    @Throws(UnknownDomainObjectException::class)
    fun getByName(name: String): T {
        return namedContainer.getByName(name)
    }
}
