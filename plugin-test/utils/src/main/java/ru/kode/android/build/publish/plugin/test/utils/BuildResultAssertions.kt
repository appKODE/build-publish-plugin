package ru.kode.android.build.publish.plugin.test.utils

import org.gradle.testkit.runner.BuildResult

/**
 * Asserts the build [output][BuildResult.getOutput] contains [text], failing with the full output on
 * mismatch. Replaces the repeated `assertTrue(result.output.contains(...))` idiom with a reusable,
 * self-describing check.
 */
fun BuildResult.outputShouldContain(text: String) {
    if (!output.contains(text)) {
        throw AssertionError("Expected build output to contain: \"$text\"\n--- actual output ---\n$output")
    }
}

/** Asserts the build output contains every one of [texts]. */
fun BuildResult.outputShouldContainAll(vararg texts: String) {
    texts.forEach { outputShouldContain(it) }
}

/** Asserts the build output does NOT contain [text], failing with the full output on mismatch. */
fun BuildResult.outputShouldNotContain(text: String) {
    if (output.contains(text)) {
        throw AssertionError("Expected build output NOT to contain: \"$text\"\n--- actual output ---\n$output")
    }
}
