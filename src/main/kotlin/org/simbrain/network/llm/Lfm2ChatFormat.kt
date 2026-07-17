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
}
