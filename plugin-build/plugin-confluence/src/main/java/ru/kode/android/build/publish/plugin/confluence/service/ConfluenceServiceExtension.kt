package ru.kode.android.build.publish.plugin.confluence.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.confluence.service.network.ConfluenceNetworkService

abstract class ConfluenceServiceExtension(
    val networkServices: Provider<Map<String, Provider<ConfluenceNetworkService>>>,
)
