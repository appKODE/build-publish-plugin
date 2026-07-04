package ru.kode.android.build.publish.plugin.jira.task.standalone

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.core.task.StandaloneServiceTask
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService
import ru.kode.android.build.publish.plugin.jira.task.standalone.work.StandaloneAddJiraLabelWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class AddJiraLabelTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
        objectFactory: ObjectFactory,
    ) : StandaloneServiceTask<JiraService>(workerExecutor) {
        init {
            description = "Adds a label to Jira issues"
        }

        @get:Input
        @get:Option(option = "label", description = "Label to add to the issues")
        abstract val label: Property<String>

        @get:Input
        @get:Option(option = "issueNumbers", description = "Jira issue keys to update")
        abstract val issueNumbers: SetProperty<String>

        @get:Input
        @get:Optional
        @get:Option(option = "instanceName", description = "Name of the Jira auth instance to use")
        abstract val instanceName: Property<String>

        @get:Internal
        val services: MapProperty<String, JiraService> =
            objectFactory.mapProperty(String::class.java, JiraService::class.java)

        @TaskAction
        fun addLabel() {
            val selectedService = resolveStandaloneJiraService(instanceName, services, service)
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(StandaloneAddJiraLabelWork::class.java) { params ->
                params.label.set(label)
                params.issues.set(issueNumbers)
                params.service.set(selectedService)
                params.loggerService.set(loggerService)
            }
        }
    }
