package ru.kode.android.build.publish.plugin.firebase.remoteconfig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.condition.ConditionEvaluator
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.condition.ConditionResult

class ConditionEvaluatorTest {
    private val evaluator = ConditionEvaluator(appVersion = "18.1.72073", appBuild = "72073")

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        delimiterString = " -> ",
        quoteCharacter = '"',
        value = [
            "true -> Match",
            "false -> NoMatch",
            "app.version.>=(['18.1']) -> Match",
            "app.version.>(['18.1.72073']) -> NoMatch",
            "app.version.<=(['18.1.72073']) -> Match",
            "app.version.<(['18.2']) -> Match",
            "app.version.==(['18.1.72073']) -> Match",
            "app.version.!=(['18.1.72073']) -> NoMatch",
            "app.version.contains(['18.1']) -> Match",
            "app.version.notContains(['18.1']) -> NoMatch",
            "app.version.exactlyMatches(['18.1.72073']) -> Match",
            "app.version.matches(['18\\.1\\..*']) -> Match",
            "app.build.>(['72000']) -> Match",
            "device.os == 'android' -> Match",
            "device.os == 'ios' -> NoMatch",
            "percent <= 10 -> Unknown",
            "percent <= 10 && app.version.<(['1.0']) -> NoMatch",
            "percent <= 10 || app.version.>=(['18.0']) -> Match",
            "percent <= 10 && app.version.>=(['18.0']) -> Unknown",
            "!(app.version.<(['18.0'])) -> Match",
            "(device.os == 'android' && app.build.<(['1'])) || true -> Match",
            "app.version.>=(['18.0' -> Unknown",
            "app.version.==(['1.0', '2.0']) -> Unknown",
        ],
    )
    fun `should evaluate expression`(
        expression: String,
        expected: ConditionResult,
    ) {
        assertEquals(expected, evaluator.evaluate(expression))
    }
}
