package ru.kode.android.build.publish.plugin.firebase.remoteconfig

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.condition.ConditionResult
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor.KeyOccurrence

/**
 * One observation of the Remote Config audit. Serialized into `report.json` with a `type` discriminator.
 */
@Serializable
sealed interface Finding {
    val type: FindingType

    /**
     * Key declared in code at the current tag but not at the previous one.
     */
    @Serializable
    @SerialName("ADDED_IN_RELEASE")
    data class AddedInRelease(val key: String, val occurrences: List<KeyOccurrence>) : Finding {
        override val type get() = FindingType.AddedInRelease
    }

    /**
     * Key declared in code at the previous tag but not at the current one.
     */
    @Serializable
    @SerialName("REMOVED_IN_RELEASE")
    data class RemovedInRelease(val key: String, val occurrences: List<KeyOccurrence>) : Finding {
        override val type get() = FindingType.RemovedInRelease
    }

    /**
     * Key declared in code but absent in the Remote Config template.
     */
    @Serializable
    @SerialName("MISSING_IN_REMOTE")
    data class MissingInRemote(val key: String, val occurrences: List<KeyOccurrence>) : Finding {
        override val type get() = FindingType.MissingInRemote
    }

    /**
     * Template parameter that no key source, usage search, dynamic prefix or ignore rule accounts for.
     */
    @Serializable
    @SerialName("UNUSED_IN_REMOTE")
    data class UnusedInRemote(val key: String, val description: String?) : Finding {
        override val type get() = FindingType.UnusedInRemote
    }

    /**
     * Conditional value whose condition refers to the app version or build and matches
     * (or may match) the audited release.
     */
    @Serializable
    @SerialName("TARGETS_RELEASE")
    data class TargetsRelease(
        val key: String,
        val condition: String,
        val expression: String,
        val result: ConditionResult,
        val value: String,
    ) : Finding {
        override val type get() = FindingType.TargetsRelease
    }

    /**
     * Template version published after the previous release tag.
     */
    @Serializable
    @SerialName("CHANGED_SINCE_RELEASE")
    data class ChangedSinceRelease(
        val versionNumber: String,
        val updateTime: String?,
        val updateUser: String?,
        val description: String?,
        val changedKeys: List<String>,
    ) : Finding {
        override val type get() = FindingType.ChangedSinceRelease
    }

    /**
     * Template parameter whose description metadata says it expired (`expires=`) or outlived the lifetime of
     * its `type=` (`created=` plus `flagLifetimes`).
     */
    @Serializable
    @SerialName("POTENTIALLY_STALE")
    data class PotentiallyStale(
        val key: String,
        val owner: String?,
        val flagType: String?,
        val reason: String,
    ) : Finding {
        override val type get() = FindingType.PotentiallyStale
    }

    /**
     * Firebase default that differs from the in-app default of the key, or `useInAppDefault` for a key
     * without an in-app default (the app then gets the static default of its type).
     *
     * @property path Defaults file of [inAppValue], `null` when the key has no in-app default.
     */
    @Serializable
    @SerialName("DEFAULTS_MISMATCH")
    data class DefaultsMismatch(
        val key: String,
        val path: String?,
        val line: Int?,
        val inAppValue: String?,
        val remoteValue: String?,
    ) : Finding {
        override val type get() = FindingType.DefaultsMismatch
    }
}
