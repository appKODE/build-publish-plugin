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
import ru.kode.android.build.publish.plugin.jira.task.standalone.work.StandaloneTransitionJiraIssueWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class TransitionJiraIssueTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : StandaloneServiceTask<JiraService>(workerExecutor) {
        init {
            description = "Transitions Jira issues to a specified status"
        }

        @get:Input
        @get:Option(option = "transitionName", description = "Name of the transition to apply")
        abstract val transitionName: Property<String>

        @get:Input
        @get:Option(option = "projectKey", description = "Jira project key (e.g. PROJECT)")
        abstract val projectKey: Property<String>

        @get:Input
        @get:Option(option = "issueNumbers", description = "Jira issue keys to transition")
        abstract val issueNumbers: SetProperty<String>

        @get:Input
        @get:Optional
        @get:Option(option = "instanceName", description = "Name of the Jira auth instance to use")
        abstract val instanceName: Property<String>

        @TaskAction
        fun transitionIssue() {
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(StandaloneTransitionJiraIssueWork::class.java) { params ->
                params.instanceName.set(instanceName)
                params.transitionName.set(transitionName)
                params.projectKey.set(projectKey)
                params.issues.set(issueNumbers)
                params.service.set(service)
                params.loggerService.set(loggerService)
            }
        }
    }
