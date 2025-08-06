package ru.kode.android.build.publish.plugin.jira.task.automation.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.util.UploadException
import ru.kode.android.build.publish.plugin.jira.service.JiraNetworkService

interface SetStatusParameters : WorkParameters {
    val issues: SetProperty<String>
    val statusTransitionId: Property<String>
    val networkService: Property<JiraNetworkService>
}

internal abstract class SetStatusWork : WorkAction<SetStatusParameters> {
    private val logger = Logging.getLogger(this::class.java)

    override fun execute() {
        val issues = parameters.issues.get()
        val service = parameters.networkService.get()
        issues.forEach { issue ->
            try {
                service.setStatus(issue, parameters.statusTransitionId.get())
            } catch (ex: UploadException) {
                logger.info("set status failed for issue $issue, error is ignored", ex)
            }
        }
    }
}
