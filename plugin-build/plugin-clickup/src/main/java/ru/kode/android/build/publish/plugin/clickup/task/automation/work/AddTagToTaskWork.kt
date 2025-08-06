package ru.kode.android.build.publish.plugin.clickup.task.automation.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.clickup.service.ClickUpNetworkService

internal interface AddTagToTaskParameters : WorkParameters {
    val tagName: Property<String>
    val issues: SetProperty<String>
    val networkService: Property<ClickUpNetworkService>
}

internal abstract class AddTagToTaskWork : WorkAction<AddTagToTaskParameters> {
    override fun execute() {
        val service = parameters.networkService.get()
        val issues = parameters.issues.get()
        val tagName = parameters.tagName.get()
        issues.forEach { issue -> service.addTagToTask(issue, tagName) }
    }
}
