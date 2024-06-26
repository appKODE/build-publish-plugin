package ru.kode.android.build.publish.plugin.task.play.publisher

/**
 * Models the possible release statuses for the track API.
 *
 * More docs are available
 * [here](https://developers.google.com/android-publisher/api-ref/edits/tracks).
 */
@Suppress("EnumNaming") // Google API Model
enum class ReleaseStatus(
    /** The API name of the status. */
    val publishedName: String,
) {
    /** The release is live. */
    COMPLETED("completed"),

    /** The release is in draft mode. */
    DRAFT("draft"),

    /** The release was aborted. */
    HALTED("halted"),

    /** The release is still being rolled out. */
    IN_PROGRESS("inProgress"),
}

/**
 * Models the possible resolution strategies for handling artifact upload conflicts.
 *
 * More docs are available
 * [here](https://github.com/Triple-T/gradle-play-publisher#handling-version-conflicts).
 */
@Suppress("EnumNaming") // Google API Model
enum class ResolutionStrategy(
    /** The API name of the strategy. */
    val publishedName: String,
) {
    /** Conflicts should be automagically resolved. */
    AUTO("auto"),

    /**
     * Unlike [AUTO] which diffs your Play Store version code with the local one, [AUTO_OFFSET] is
     * much simpler and just adds the local version code to the Play Store one when
     * `local <= play_store`.
     */
    AUTO_OFFSET("auto_offset"),

    /** Fail the build at the first sign of conflict. */
    FAIL("fail"),

    /** Keep going and pretend like nothing happened. */
    IGNORE("ignore"),
}
