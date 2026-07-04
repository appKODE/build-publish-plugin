package ru.kode.android.build.publish.plugin.sender.task.jira

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.sender.task.base.BaseBasicAuthSenderTask
import ru.kode.android.build.publish.plugin.sender.task.jira.work.AddJiraFixVersionWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class AddJiraFixVersionTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : BaseBasicAuthSenderTask(workerExecutor) {
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

        @TaskAction
        fun addFixVersion() {
            workerExecutor.noIsolation().submit(AddJiraFixVersionWork::class.java) { params ->
                params.baseUrl.set(baseUrl)
                params.username.set(username)
                params.apiToken.set(apiToken)
                params.version.set(version)
                params.projectKey.set(projectKey)
                params.issues.set(issueNumbers)
            }
        }
    }
