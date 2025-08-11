package ru.kode.android.build.publish.plugin.slack.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

abstract class SlackDistributionConfig {
    abstract val name: String

    /**
     * Api token file to upload files in slack
     */
    @get:InputFile
    abstract val uploadApiTokenFile: RegularFileProperty

    /**
     * Public channels where file will be uploaded
     */
    @get:Optional
    @get:Input
    internal abstract val destinationChannels: SetProperty<String>

    fun destinationChannel(channelId: String) {
        destinationChannels.add(channelId)
    }
}
