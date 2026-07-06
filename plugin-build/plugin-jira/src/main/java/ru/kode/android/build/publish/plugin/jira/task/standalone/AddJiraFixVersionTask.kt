package ru.kode.android.build.publish.plugin.jira.task.standalone

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.core.task.StandaloneServiceTask
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService
import ru.kode.android.build.publish.plugin.jira.task.standalone.work.StandaloneAddJiraFixVersionWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class AddJiraFixVersionTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : StandaloneServiceTask<JiraService>(workerExecutor) {
        init {
            description = "Adds a fix version to Jira issues"
        }

        @get:Input
        @get:Option(option = "version", description = "Version name to set as fix version")
        abstract val version: Property<String>

        @get:Input
        @get:Option(option = "projectKey", description = "Jira project key (e.g. PROJECT)")
        abstract val projectKey: Property<String>

        @get:Input
        @get:Option(option = "issueNumbers", description = "Jira issue keys to update")
        abstract val issueNumbers: SetProperty<String>

        @get:Input
        @get:Optional
        @get:Option(option = "instanceName", description = "Name of the Jira auth instance to use")
        abstract val instanceName: Property<String>

        @TaskAction
        fun addFixVersion() {
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(StandaloneAddJiraFixVersionWork::class.java) { params ->
                params.instanceName.set(instanceName)
                params.version.set(version)
                params.projectKey.set(projectKey)
                params.issues.set(issueNumbers)
                params.service.set(service)
                params.loggerService.set(loggerService)
            }
        }
    }
