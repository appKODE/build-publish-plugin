package ru.kode.android.build.publish.plugin.clickup.service.network

import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpController
import ru.kode.android.build.publish.plugin.clickup.controller.addFixVersionToTasks
import ru.kode.android.build.publish.plugin.clickup.controller.addTagToTasks
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpAccountEntity
import ru.kode.android.build.publish.plugin.clickup.controller.factory.ClickUpControllerFactory
import ru.kode.android.build.publish.plugin.clickup.controller.mappers.clickUpAccountFromJson
import ru.kode.android.build.publish.plugin.clickup.messages.unknownAccountNameMessage
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.util.COMMON_CONTAINER_NAME
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * A Gradle build service providing network operations for every configured ClickUp account.
 *
 * Mirrors the Jira single-service model: one service per build variant bakes **all** of that variant's
 * accounts (token + projects) as JSON into its parameters, and each operation selects the account by
 * name. A [ClickUpController] is built lazily per account on first use (each account has its own token,
 * so accounts cannot share one controller).
 *
 * @see ClickUpController
 */
abstract class ClickUpService
    @Inject
    constructor() : BuildService<ClickUpService.Params> {
        /**
         * Configuration parameters for the [ClickUpService].
         */
        interface Params : BuildServiceParameters {
            /** Each configured ClickUp account, baked as [ClickUpAccountEntity] JSON. */
            val accounts: ListProperty<String>

            /**
             * A Gradle service that provides logging capabilities for the [ClickUpService].
             *
             * @see LoggerService
             */
            val loggerService: Property<LoggerService>
        }

        private val accounts: List<ClickUpAccountEntity> by lazy {
            parameters.accounts.get().map { clickUpAccountFromJson(it) }
        }

        private val controllers = ConcurrentHashMap<String, ClickUpController>()
        private val teamIds = ConcurrentHashMap<String, String>()

        /**
         * Resolves the controller for [requestedAccountName]. A blank name (a standalone task invoked
         * without `--accountName`) selects the account named `default`, or the sole/first declared account
         * otherwise.
         */
        private fun controller(requestedAccountName: String): ClickUpController {
            val accountName = requestedAccountName.ifBlank { defaultAccountName() }
            return controllers.getOrPut(accountName) {
                val account =
                    accounts.firstOrNull { it.name == accountName }
                        ?: throw GradleException(unknownAccountNameMessage(accountName, accounts.map { it.name }))
                ClickUpControllerFactory.build(
                    token = account.token,
                    logger = parameters.loggerService.get().logger,
                )
            }
        }

        private fun defaultAccountName(): String =
            (accounts.firstOrNull { it.name == COMMON_CONTAINER_NAME } ?: accounts.firstOrNull())?.name
                ?: throw GradleException(unknownAccountNameMessage(COMMON_CONTAINER_NAME, emptyList()))

        /**
         * Resolves a ClickUp workspace (team) name to its team id on [accountName], caching the result.
         * Returns `null` when no matching team is reachable by the account's token.
         */
        fun getTeamId(
            accountName: String,
            workspaceName: String,
        ): String? =
            teamIds.getOrPut("$accountName/$workspaceName") {
                controller(accountName).getTeamId(workspaceName) ?: return null
            }

        /**
         * Retrieves the ID of a ClickUp custom field on [accountName].
         */
        fun getCustomFieldId(
            accountName: String,
            workspaceName: String,
            fieldName: String,
        ): String = controller(accountName).getOrCreateCustomFieldId(workspaceName, fieldName)

        /**
         * Retrieves the name (title) of a ClickUp task on [accountName], or `null` when it cannot be
         * retrieved. When [teamId] is non-null, [taskId] is resolved as a custom task id scoped to that
         * team.
         */
        fun getTaskName(
            accountName: String,
            taskId: String,
            teamId: String? = null,
        ): String? = controller(accountName).getTaskName(taskId, teamId)

        /**
         * Adds a tag to a ClickUp task on [accountName].
         */
        fun addTagToTask(
            accountName: String,
            taskId: String,
            tagName: String,
        ) = controller(accountName).addTagToTask(taskId, tagName)

        /**
         * Adds or updates a custom field value for a ClickUp task on [accountName].
         */
        fun addFieldToTask(
            accountName: String,
            taskId: String,
            fieldId: String,
            fieldValue: String,
        ) = controller(accountName).addFieldToTask(taskId, fieldId, fieldValue)

        /**
         * Adds a tag to multiple ClickUp tasks on [accountName].
         */
        fun addTagToTasks(
            accountName: String,
            tagName: String,
            taskIds: Collection<String>,
            log: (String) -> Unit,
        ) = controller(accountName).addTagToTasks(tagName, taskIds, log)

        /**
         * Adds or updates the fix version custom field for multiple ClickUp tasks on [accountName].
         */
        fun addFixVersionToTasks(
            accountName: String,
            workspaceName: String,
            fieldName: String,
            version: String,
            taskIds: Collection<String>,
            log: (String) -> Unit,
        ) = controller(accountName).addFixVersionToTasks(workspaceName, fieldName, version, taskIds, log)
    }
