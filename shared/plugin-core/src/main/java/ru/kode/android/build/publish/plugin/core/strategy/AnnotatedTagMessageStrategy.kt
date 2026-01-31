package ru.kode.android.build.publish.plugin.core.strategy

/**
 * A strategy interface for formatting annotated tag messages in the changelog.
 *
 * Implementations of this interface define how annotated tag messages are
 * transformed before being included in the generated changelog output.
 */
interface AnnotatedTagMessageStrategy {
    /**
     * Builds a formatted string from the given annotated tag message.
     *
     * @param annotatedTagMessage The raw annotated tag message from Git.
     * @return The formatted message to include in the changelog.
     */
    fun build(annotatedTagMessage: String): String
}

/**
 * A default implementation of [AnnotatedTagMessageStrategy] that decorates
 * the annotated tag message with asterisks for emphasis.
 *
 * This strategy wraps the message in asterisks (*), which is commonly used
 * for bold or italic formatting in Markdown and other text formats.
 */
object DecoratedAnnotatedTagMessageStrategy : AnnotatedTagMessageStrategy {
    /**
     * Formats the annotated tag message by wrapping it with asterisks.
     *
     * @param annotatedTagMessage The raw annotated tag message from Git.
     * @return The message wrapped in asterisks for emphasis (e.g., `*message*`).
     */
    override fun build(annotatedTagMessage: String): String {
        return "*$annotatedTagMessage*"
    }
}
