package com.example.customplugin

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import ru.kode.android.build.publish.plugin.core.api.container.BuildPublishDomainObjectContainer
import ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension
import ru.kode.android.build.publish.plugin.core.enity.ExtensionInput
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.getByNameOrRequiredCommon
import javax.inject.Inject
import com.android.build.api.variant.ApplicationVariant

private const val EXTENSION_NAME = "buildPublishPrintTag"

class CustomBuildPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(
            EXTENSION_NAME,
            CustomBuildPublishExtension::class.java
        )
    }
}

abstract class CustomBuildPublishExtension
    @Inject
    constructor(objectFactory: ObjectFactory) : BuildPublishConfigurableExtension() {

        internal val message: NamedDomainObjectContainer<MessageConfig> =
            objectFactory.domainObjectContainer(MessageConfig::class.java)

        val messageConfig: (buildName: String) -> MessageConfig = { buildName ->
            message.getByNameOrRequiredCommon(buildName)
        }

        fun message(configurationAction: Action<BuildPublishDomainObjectContainer<MessageConfig>>) {
            val container = BuildPublishDomainObjectContainer(message)
            configurationAction.execute(container)
        }

        fun messageCommon(configurationAction: Action<MessageConfig>) {
            common(message, configurationAction)
        }

        override fun configure(project: Project, input: ExtensionInput, variant: ApplicationVariant) {
            val messageConfig = messageConfig(input.buildVariant.name)

            project.tasks.register(
                "printTagExample${input.buildVariant.capitalizedName()}",
                PrintLastTagTask::class.java
            ) {
                it.buildTagFile.set(input.output.lastBuildTagFile)
                it.message.set(messageConfig.additionalText)
            }
        }
    }

interface MessageConfig {
    val name: String

    @get:Input
    val additionalText: Property<String>
}

abstract class PrintLastTagTask
    @Inject
    constructor() : DefaultTask() {
        init {
            description = "Task to print last tag"
            group = BasePlugin.BUILD_GROUP
            outputs.upToDateWhen { false }
        }

        @get:Input
        @get:Option(
            option = "message",
            description = "Custom message which will be printed",
        )
        abstract val message: Property<String>

        @get:InputFile
        @get:Option(
            option = "buildTagFile",
            description = "Path to a JSON file containing build metadata (name, number, variant).",
        )
        abstract val buildTagFile: RegularFileProperty

        @TaskAction
        fun getLastTag() {
            val currentBuildTag = fromJson(buildTagFile.asFile.get())
            println("Last tag name: ${currentBuildTag.name} (message: ${message.get()})")
        }
    }