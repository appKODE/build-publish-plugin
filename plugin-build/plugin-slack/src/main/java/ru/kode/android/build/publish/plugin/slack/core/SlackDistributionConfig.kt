package ru.kode.android.build.publish.plugin.slack.core

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

interface SlackDistributionConfig {
    val name: String

    /**
     * Api token file to upload files in slack
     */
    @get:InputFile
    val uploadApiTokenFile: RegularFileProperty

    /**
     * Public channels where file will be uploaded
     */
    @get:Optional
    @get:Input
    val uploadChannels: SetProperty<String>

}
