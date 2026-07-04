package ru.kode.android.build.publish.plugin.jira

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.jira.controller.JiraController
import ru.kode.android.build.publish.plugin.jira.controller.factory.JiraControllerFactory

class JiraControllerImplTest {
    private val server = MockWebServer()
    private val logs = mutableListOf<String>()

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    private fun controller(): JiraController {
        val baseUrl = server.url("/").toString().trimEnd('/')
        return JiraControllerFactory.build(
            baseUrl = baseUrl,
            username = "user",
            password = "pass",
            log = { msg -> logs.add(msg) },
        )
    }

    @Test
    fun `getProjectId returns id from project endpoint`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":42,"key":"PROJ","name":"Project"}"""),
        )

        val id = controller().getProjectId("PROJ")

        assertEquals(42L, id)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/rest/api/2/project/PROJ", request.path)
    }

    @Test
    fun `addIssueLabel sends PUT with label update in body`() {
        server.enqueue(MockResponse().setResponseCode(204))

        controller().addIssueLabel(issue = "PROJ-1", label = "release")

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/rest/api/2/issue/PROJ-1", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("labels"), "Body should contain labels update")
        assertTrue(body.contains("release"), "Body should contain the label value")
    }

    @Test
    fun `addIssueFixVersion sends PUT with fix version in body`() {
        server.enqueue(MockResponse().setResponseCode(204))

        controller().addIssueFixVersion(issue = "PROJ-1", version = "1.0.0")

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/rest/api/2/issue/PROJ-1", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("fixVersions"), "Body should contain fixVersions update")
        assertTrue(body.contains("1.0.0"), "Body should contain the version name")
    }

    @Test
    fun `createProjectVersion posts version to version endpoint`() {
        server.enqueue(MockResponse().setResponseCode(201))

        controller().createProjectVersion(projectId = 42L, version = "1.0.0")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/rest/api/2/version", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("1.0.0"), "Body should contain the version name")
        assertTrue(body.contains("42"), "Body should contain the project id")
    }

    @Test
    fun `setIssueStatus posts transition id to transitions endpoint`() {
        server.enqueue(MockResponse().setResponseCode(204))

        controller().setIssueStatus(issue = "PROJ-1", statusTransitionId = "31")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/rest/api/2/issue/PROJ-1/transitions", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("31"), "Body should contain the transition id")
    }

    @Test
    fun `getStatusTransitionId resolves transition id from statuses and transitions`() {
        // getProjectAvailableStatuses
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"id":"wf1","name":"Default","statuses":[{"id":"10","name":"Done"}]}]"""),
        )
        // getAvailableTransitions is requested twice by the implementation
        val transitionsBody = """{"transitions":[{"id":"31","name":"Finish","to":{"id":"10","name":"Done"}}]}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(transitionsBody))
        server.enqueue(MockResponse().setResponseCode(200).setBody(transitionsBody))

        val transitionId =
            controller().getStatusTransitionId(
                projectKey = "PROJ",
                statusName = "Done",
                issues = listOf("PROJ-1"),
            )

        assertEquals("31", transitionId)
        val firstRequest = server.takeRequest()
        assertEquals("GET", firstRequest.method)
        assertEquals("/rest/api/2/project/PROJ/statuses", firstRequest.path)
    }
}
