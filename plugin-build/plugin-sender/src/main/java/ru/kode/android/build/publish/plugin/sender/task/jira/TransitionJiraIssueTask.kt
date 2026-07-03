package ru.kode.android.build.publish.plugin.sender.task.jira

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.sender.task.base.BaseBasicAuthSenderTask
import ru.kode.android.build.publish.plugin.sender.task.jira.work.TransitionJiraIssueWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class TransitionJiraIssueTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : BaseBasicAuthSenderTask(workerExecutor) {
        init {
            description = "Transitions Jira issues to a new status"
        }

        @get:Input
        @get:Option(option = "transitionName", description = "Target status name to transition to")
        abstract val transitionName: Property<String>

        @get:Input
        @get:Option(option = "projectKey", description = "Jira project key (e.g. PROJECT)")
        abstract val projectKey: Property<String>

        @get:Input
        @get:Option(option = "issueNumbers", description = "Jira issue keys to transition")
        abstract val issueNumbers: SetProperty<String>

        @TaskAction
        fun transitionIssues() {
            workerExecutor.noIsolation().submit(TransitionJiraIssueWork::class.java) { params ->
                params.baseUrl.set(baseUrl)
                params.username.set(username)
                params.apiToken.set(apiToken)
                params.transitionName.set(transitionName)
                params.projectKey.set(projectKey)
                params.issues.set(issueNumbers)
            }
        }
    }
