package ru.kode.android.build.publish.plugin.core.task

/**
 * Canonical Gradle task names for every plugin, kept in one place so plugins, the aggregator
 * `plugin-sender`, and tests all reference a single source of truth instead of duplicating string
 * literals.
 *
 * Two kinds of constants:
 * - full task names (e.g. [Slack.SEND_MESSAGE]) — used verbatim.
 * - `*_PREFIX` names (e.g. [Slack.SEND_CHANGELOG_PREFIX]) — per-variant tasks whose final name is the
 *   prefix followed by the capitalized build-variant name (`"$PREFIX${variant.capitalizedName()}"`).
 */
object TaskNames {
    object Foundation {
        const val GENERATE_CHANGELOG_PREFIX = "generateChangelog"
        const val PRINT_LAST_INCREASED_TAG_PREFIX = "printLastIncreasedTag"
        const val RENAME_APK_PREFIX = "renameApk"
        const val RENAME_BUNDLE_PREFIX = "renameBundle"
        const val GET_LAST_TAG_SNAPSHOT_PREFIX = "getLastTagSnapshot"
        const val COMPUTE_VERSION_CODE_PREFIX = "computeVersionCode"
        const val COMPUTE_APK_OUTPUT_FILENAME_PREFIX = "computeApkOutputFileName"
        const val COMPUTE_BUNDLE_OUTPUT_FILENAME_PREFIX = "computeBundleOutputFileName"
        const val COMPUTE_VERSION_NAME_PREFIX = "computeVersionName"
    }

    object Slack {
        const val SEND_MESSAGE = "sendSlackMessage"
        const val SEND_FILE = "sendSlackFile"
        const val SEND_CHANGELOG_PREFIX = "sendSlackChangelog"
        const val DISTRIBUTION_UPLOAD_PREFIX = "slackDistributionUpload"
        const val DISTRIBUTION_UPLOAD_BUNDLE_PREFIX = "slackDistributionUploadBundle"
    }

    object Telegram {
        const val SEND_MESSAGE = "sendTelegramMessage"
        const val SEND_FILE = "sendTelegramFile"
        const val SEND_CHANGELOG_PREFIX = "sendTelegramChangelog"
        const val DISTRIBUTION_UPLOAD_PREFIX = "telegramDistributionUpload"
        const val DISTRIBUTION_UPLOAD_BUNDLE_PREFIX = "telegramDistributionUploadBundle"
        const val LOOKUP_PREFIX = "telegramLookup"
    }

    object Nextcloud {
        const val UPLOAD = "uploadToNextcloud"
        const val DISTRIBUTION_UPLOAD_PREFIX = "nextcloudDistributionUpload"
        const val DISTRIBUTION_UPLOAD_BUNDLE_PREFIX = "nextcloudDistributionUploadBundle"
        const val CHANGELOG_UPLOAD_PREFIX = "nextcloudChangelogUpload"
    }

    object Confluence {
        const val UPLOAD = "uploadToConfluence"
        const val ADD_COMMENT = "addConfluenceComment"
        const val DISTRIBUTION_UPLOAD_PREFIX = "confluenceDistributionUpload"
        const val DISTRIBUTION_UPLOAD_BUNDLE_PREFIX = "confluenceDistributionUploadBundle"
    }

    object Jira {
        const val ADD_FIX_VERSION = "addJiraFixVersion"
        const val ADD_LABEL = "addJiraLabel"
        const val TRANSITION_ISSUE = "transitionJiraIssue"
        const val AUTOMATION_PREFIX = "jiraAutomation"
    }

    object ClickUp {
        const val ADD_TAG = "addClickUpTag"
        const val ADD_FIX_VERSION = "addClickUpFixVersion"
        const val AUTOMATION_PREFIX = "clickUpAutomation"
    }

    object Play {
        const val DISTRIBUTION_UPLOAD_PREFIX = "playUpload"
    }
}
