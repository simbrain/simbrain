/**
 * Token text for compact readouts: whitespace becomes the conventional editor symbols, so a
 * space, newline, or leading space is legible without wrapping the token in quotes.
 */
package org.simbrain.network.compositor

/** [token] with each whitespace character replaced by a visible symbol. */
fun visibleToken(token: String): String = buildString {
    for (ch in token) {
        append(
            when (ch) {
                ' ' -> '␣'
                '\n' -> '↵'
                '\r' -> '␍'
                '\t' -> '⇥'
                else -> if (Character.isWhitespace(ch) || Character.isSpaceChar(ch)) '·' else ch
            }
        )
    }
}
