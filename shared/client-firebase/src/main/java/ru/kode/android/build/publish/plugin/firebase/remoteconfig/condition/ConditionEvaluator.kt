package ru.kode.android.build.publish.plugin.firebase.remoteconfig.condition

private val VERSION_ATOM =
    Regex(
        """^app\.(version|build)\.(<=|>=|==|!=|<|>|contains|notContains|exactlyMatches|matches)""" +
            """\(\[(.*)]\)$""",
    )
private val OS_ATOM = Regex("""^device\.os\s*==\s*'([^']*)'$""")
private val QUOTED = Regex("""'((?:[^'\\]|\\.)*)'|"((?:[^"\\]|\\.)*)"""")

/**
 * Evaluates a Firebase Remote Config condition expression for an Android build with the given
 * [appVersion] (`versionName`) and [appBuild] (`versionCode`).
 *
 * Supported: `&&`, `||`, `!`, parentheses, `true`/`false`, `device.os == '...'` and
 * `app.version` / `app.build` with `<`, `<=`, `==`, `!=`, `>`, `>=`, `contains`, `notContains`,
 * `exactlyMatches`, `matches` (e.g. `app.version.>=(['1.2.0'])`). Every other atom is
 * [ConditionResult.Unknown], so the result never claims more than can be known at build time.
 */
class ConditionEvaluator(
    private val appVersion: String?,
    private val appBuild: String?,
) {
    fun evaluate(expression: String): ConditionResult =
        runCatching { Parser(expression).parse() }
            .getOrDefault(ConditionResult.Unknown)

    private inner class Parser(private val text: String) {
        private var pos = 0

        fun parse(): ConditionResult {
            val result = or()
            skipSpaces()
            require(pos == text.length) { "Unexpected '${text.substring(pos)}'" }
            return result
        }

        private fun or(): ConditionResult {
            var result = and()
            while (consume("||")) result = result or and()
            return result
        }

        private fun and(): ConditionResult {
            var result = unary()
            while (consume("&&")) result = result and unary()
            return result
        }

        private fun unary(): ConditionResult =
            when {
                consume("!") -> !unary()
                consume("(") -> or().also { require(consume(")")) { "Missing ')'" } }
                else -> atom(readAtom())
            }

        private fun readAtom(): String {
            skipSpaces()
            val start = pos
            var depth = 0
            var quote: Char? = null
            while (pos < text.length && (quote != null || !atAtomEnd(depth))) {
                val char = text[pos]
                when {
                    quote != null ->
                        if (char == '\\') {
                            pos++
                        } else if (char == quote) {
                            quote = null
                        }
                    char == '\'' || char == '"' -> quote = char
                    char == '(' || char == '[' -> depth++
                    char == ')' || char == ']' -> depth--
                }
                pos++
            }
            return text.substring(start, pos).trim().also { require(it.isNotEmpty()) { "Empty atom at $start" } }
        }

        private fun atAtomEnd(depth: Int): Boolean =
            depth == 0 &&
                (text[pos] == ')' || text[pos] == ']' || text.startsWith("&&", pos) || text.startsWith("||", pos))

        private fun consume(token: String): Boolean {
            skipSpaces()
            return text.startsWith(token, pos).also { if (it) pos += token.length }
        }

        private fun skipSpaces() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }
    }

    private fun atom(atom: String): ConditionResult {
        val os = OS_ATOM.matchEntire(atom)?.groupValues?.get(1)
        val version = VERSION_ATOM.matchEntire(atom)
        return when {
            atom == "true" -> ConditionResult.Match
            atom == "false" -> ConditionResult.NoMatch
            os != null -> (os == "android").toResult()
            version != null -> versionAtom(version)
            else -> ConditionResult.Unknown
        }
    }

    private fun versionAtom(match: MatchResult): ConditionResult {
        val (field, operator, list) = match.destructured
        val actual = (if (field == "version") appVersion else appBuild) ?: return ConditionResult.Unknown
        val values = QUOTED.findAll(list).map { it.groupValues[1].ifEmpty { it.groupValues[2] } }.toList()
        return when (operator) {
            "contains" -> values.any { actual.contains(it) }
            "notContains" -> values.none { actual.contains(it) }
            "exactlyMatches" -> values.any { actual == it }
            "matches" -> values.any { Regex(it).matches(actual) }
            else ->
                compareVersions(actual, values.single()).let { cmp ->
                    when (operator) {
                        "<" -> cmp < 0
                        "<=" -> cmp <= 0
                        ">" -> cmp > 0
                        ">=" -> cmp >= 0
                        "==" -> cmp == 0
                        else -> cmp != 0
                    }
                }
        }.toResult()
    }
}

/**
 * Compares dot-separated versions segment by segment, numerically where both segments are numbers;
 * missing segments count as `0` (`1.2` == `1.2.0`).
 */
internal fun compareVersions(
    left: String,
    right: String,
): Int {
    val leftParts = left.split('.')
    val rightParts = right.split('.')
    for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
        val l = leftParts.getOrElse(index) { "0" }
        val r = rightParts.getOrElse(index) { "0" }
        val ln = l.toLongOrNull()
        val rn = r.toLongOrNull()
        val cmp = if (ln != null && rn != null) ln.compareTo(rn) else l.compareTo(r)
        if (cmp != 0) return cmp
    }
    return 0
}

private fun Boolean.toResult() = if (this) ConditionResult.Match else ConditionResult.NoMatch
