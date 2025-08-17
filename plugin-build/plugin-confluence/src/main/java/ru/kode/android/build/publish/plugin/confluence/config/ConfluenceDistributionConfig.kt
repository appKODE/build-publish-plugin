package ru.kode.android.build.publish.plugin.confluence.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

/**
 * Configuration for publishing Android build information to Confluence.
 *
 * This interface defines the settings required to publish build information,
 * such as release notes or version details, to a Confluence page.
 */
interface ConfluenceDistributionConfig {
    val name: String

    /**
     * The ID of the Confluence page where the build will be published.
     *
     * This is a required property that specifies the target page for publishing.
     * The page ID can be found in the URL when viewing the page in Confluence.
     *
     * Example: For a page with URL `https://your-domain.atlassian.net/wiki/spaces/SPACE/pages/12345678/Page+Title`,
     * the page ID would be "12345678".
     */
    @get:Input
    val pageId: Property<String>
}
