package ru.kode.android.build.publish.plugin.sender.task.jira

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.sender.task.base.BaseBasicAuthSenderTask
import ru.kode.android.build.publish.plugin.sender.task.jira.work.AddJiraLabelWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class AddJiraLabelTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : BaseBasicAuthSenderTask(workerExecutor) {
        init {
            description = "Adds a label to Jira issues"
        }

        @get:Input
        @get:Option(option = "label", description = "Label to add to the issues")
        abstract val label: Property<String>

        @get:Input
        @get:Option(option = "issueNumbers", description = "Jira issue keys to update")
        abstract val issueNumbers: SetProperty<String>

        @TaskAction
        fun addLabel() {
            workerExecutor.noIsolation().submit(AddJiraLabelWork::class.java) { params ->
                params.baseUrl.set(baseUrl)
                params.username.set(username)
                params.apiToken.set(apiToken)
                params.label.set(label)
                params.issues.set(issueNumbers)
            }
        }
    }
