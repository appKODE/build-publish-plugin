package ru.kode.android.build.publish.plugin.firebase.remoteconfig.condition

/**
 * Three-valued result: [Unknown] means the condition depends on something only known on a device
 * (percentiles, audiences, user properties, country, ...).
 */
enum class ConditionResult {
    Match,
    NoMatch,
    Unknown,
    ;

    infix fun and(other: ConditionResult): ConditionResult =
        when {
            this == NoMatch || other == NoMatch -> NoMatch
            this == Match && other == Match -> Match
            else -> Unknown
        }

    infix fun or(other: ConditionResult): ConditionResult =
        when {
            this == Match || other == Match -> Match
            this == NoMatch && other == NoMatch -> NoMatch
            else -> Unknown
        }

    operator fun not(): ConditionResult =
        when (this) {
            Match -> NoMatch
            NoMatch -> Match
            Unknown -> Unknown
        }
}
