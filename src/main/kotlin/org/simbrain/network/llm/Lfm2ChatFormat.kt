package org.simbrain.network.llm

/**
 * The LFM2 chat wire format, translated from the model's `chat_template.jinja`. Only the
 * append side of the template is needed: the context is an append-only token stream, so
 * history is never re-rendered. Strings built here already carry their special-token text
 * ([BOS], turn markers) and must be encoded with specials off — [LlmTokenizer] maps the
 * literal marker text back to single ids.
 *
 * Tool conventions (from the same template, for later phases): the tool list is plain text in
 * the system turn (`List of tools: [{json}, …]`); the model emits calls Python-style between
 * the tool-call markers, which are deliberately non-special so they survive skip-specials
 * decoding; results go back as a `tool` role turn wrapping the response markers.
 */
object Lfm2ChatFormat {

    const val BOS_ID = 1
    const val IM_START_ID = 6
    const val IM_END_ID = 7
    const val TOOL_CALL_START_ID = 10
    const val TOOL_CALL_END_ID = 11

    const val BOS = "<|startoftext|>"
    const val IM_START = "<|im_start|>"
    const val IM_END = "<|im_end|>"
    const val TOOL_RESPONSE_START = "<|tool_response_start|>"
    const val TOOL_RESPONSE_END = "<|tool_response_end|>"

    /** The open assistant turn that cues the model to answer. */
    const val GENERATION_PROMPT = "${IM_START}assistant\n"

    fun turn(role: String, content: String) = "$IM_START$role\n$content$IM_END\n"

    /**
     * A complete single-turn chat prompt: BOS, an optional system turn, the user turn, and the
     * open assistant turn. Matches Python `apply_chat_template(add_generation_prompt = true)`.
     */
    fun chatPrompt(userText: String, systemText: String = "") = buildString {
        append(BOS)
        if (systemText.isNotEmpty()) append(turn("system", systemText))
        append(turn("user", userText))
        append(GENERATION_PROMPT)
    }

    /** A tool-result turn ready to inject mid-run, reopening the assistant turn after it. */
    fun toolResultTurn(result: String) =
        turn("tool", "$TOOL_RESPONSE_START$result$TOOL_RESPONSE_END") + GENERATION_PROMPT

    /** The plain-text tool advertisement the template puts in the system turn. */
    fun toolListLine(tools: List<LlmTool>) =
        tools.joinToString(", ", prefix = "List of tools: [", postfix = "]") { it.schemaJson() }

    data class ToolCall(val name: String, val arguments: Map<String, String>)

    private val callPattern = Regex("""([A-Za-z_][A-Za-z0-9_]*)\s*\(([^)]*)\)""")

    /**
     * Parses the model's Python-style call list, e.g. `[get_weather(location='Boston, MA')]` —
     * the text between the tool-call markers. The template renders string arguments
     * single-quoted but the model also emits double quotes in practice; both are stripped and
     * commas inside them survive. Other values are kept verbatim. A tiny parser, not a
     * grammar: nested parentheses inside argument values are out of scope.
     */
    fun parseToolCalls(raw: String): List<ToolCall> = callPattern.findAll(raw).map { match ->
        val arguments = splitArgs(match.groupValues[2])
            .mapNotNull { pair ->
                val parts = pair.split("=", limit = 2).map { it.trim() }
                if (parts.size == 2) {
                    parts[0] to parts[1].removeSurrounding("'").removeSurrounding("\"")
                } else null
            }
            .toMap()
        ToolCall(match.groupValues[1], arguments)
    }.toList()

    private fun splitArgs(raw: String): List<String> {
        val parts = ArrayList<String>()
        val current = StringBuilder()
        var quote: Char? = null
        for (c in raw) {
            when {
                quote != null -> {
                    current.append(c)
                    if (c == quote) quote = null
                }
                c == '\'' || c == '"' -> {
                    current.append(c)
                    quote = c
                }
                c == ',' -> {
                    parts.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
        }
        if (current.isNotBlank()) parts.add(current.toString())
        return parts
    }
}

/**
 * A tool the model can call in chat mode: advertised in the system turn as a JSON schema,
 * invoked with the parsed argument map, returning plain text for the tool-result turn.
 * [parameters] is the JSON-schema `parameters` object as a raw string.
 */
class LlmTool(
    val name: String,
    val description: String,
    val parameters: String,
    val execute: (Map<String, String>) -> String,
) {
    fun schemaJson(): String =
        """{"type": "function", "function": {"name": "$name", "description": "$description", """ +
            """"parameters": $parameters}}"""
}
