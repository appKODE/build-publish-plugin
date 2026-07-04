package ru.kode.android.build.publish.plugin.test.utils

/**
 * Polls [condition] until it returns `true` or [timeoutMs] elapses, sleeping [intervalMs] between
 * attempts. Absorbs read-after-write lag when verifying state on a live server right after a mutation:
 * e.g. Jira's REST label / fix-version / status reads are eventually consistent, so an immediate
 * re-read can still be stale even though the preceding write returned success.
 *
 * Throws [AssertionError] with [message] if the condition never holds within the timeout, so it is a
 * drop-in replacement for `assertTrue { condition }` at a live-state verification site — just move the
 * read inside the lambda so it is re-evaluated on each attempt.
 */
fun awaitUntil(
    message: String,
    timeoutMs: Long = 10_000,
    intervalMs: Long = 300,
    condition: () -> Boolean,
) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (true) {
        if (condition()) return
        if (System.currentTimeMillis() >= deadline) {
            throw AssertionError("Timed out after ${timeoutMs}ms waiting for: $message")
        }
        Thread.sleep(intervalMs)
    }
}
