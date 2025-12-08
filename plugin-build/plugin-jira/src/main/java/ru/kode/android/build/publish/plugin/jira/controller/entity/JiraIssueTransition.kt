package ru.kode.android.build.publish.plugin.jira.controller.entity

data class JiraIssueTransition(
    val id: String,
    val name: String,
    val statusId: String
)