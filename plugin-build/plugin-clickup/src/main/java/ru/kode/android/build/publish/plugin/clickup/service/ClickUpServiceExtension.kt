package ru.kode.android.build.publish.plugin.clickup.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpNetworkService

abstract class ClickUpServiceExtension(
    val networkServices: Provider<Map<String, Provider<ClickUpNetworkService>>>,
)
