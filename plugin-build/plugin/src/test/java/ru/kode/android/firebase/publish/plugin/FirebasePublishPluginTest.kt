package ru.kode.android.firebase.publish.plugin

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import ru.kode.android.firebase.publish.plugin.task.SendChangelogTask

class FirebasePublishPluginTest {

    @Test
    fun `plugin is applied correctly to the project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("ru.kode.android.firebase-publish-plugin")
        val firebasePublishExt = project.extensions.getByType(FirebasePublishExtension::class.java)
        val androidExt = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
        androidExt.finalizeDsl {
            it.buildTypes.register("debug")
            it.buildTypes.register("release")
        }
        println("tasks = ${project.tasks.map { it.name }}")
        assert(project.tasks.getByName("${SEND_CHANGELOG_TASK_PREFIX}Debug") is SendChangelogTask)
    }

    @Test
    fun `extension templateExampleConfig is created correctly`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("ru.kode.android.firebase-publish-plugin")
        assertNotNull(project.extensions.getByName(EXTENSION_NAME))
    }

    @Test
    fun `parameters are passed correctly from extension to task`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("ru.kode.android.firebase-publish-plugin")
        (project.extensions.getByName(EXTENSION_NAME) as FirebasePublishExtension)
        val task =
            project.tasks.getByName("${SEND_CHANGELOG_TASK_PREFIX}Debug") as SendChangelogTask

        assertEquals("buildTypes", task.buildVariants.get())
    }
}
