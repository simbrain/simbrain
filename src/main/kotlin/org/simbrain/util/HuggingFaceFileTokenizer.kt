package org.simbrain.util

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import org.simbrain.util.propertyeditor.GuiEditable
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Implemented by text producers whose output has a definite tokenization, so a consuming
 * display can draw honest token boundaries. TextWorld adopts the tokenizer when a document
 * coupling from such a producer is created.
 */
interface ProvidesDisplayTokenizer {
    val displayTokenizer: Tokenizer<*>
}

/**
 * A [Tokenizer] backed by a HuggingFace `tokenizer.json`, reporting each subword token's true
 * character span (specials are not added — text carrying literal marker scaffolding tokenizes
 * to single markers). Token text comes from the source substring, not the raw BPE piece, so
 * results never show byte-level artifacts. Serializes as the file path; the native handle
 * loads lazily, and a missing or unreadable file tokenizes to nothing rather than failing.
 */
class HuggingFaceFileTokenizer(path: String = "") : Tokenizer<HuggingFaceFileTokenizer>() {

    var path by GuiEditable(
        initValue = path,
        label = "Tokenizer file",
        description = "Path to a HuggingFace tokenizer.json",
        useFileChooser = true,
        order = 10,
    )

    @Transient
    private var loaded: Pair<String, HuggingFaceTokenizer>? = null

    private fun handle(): HuggingFaceTokenizer? {
        loaded?.let { if (it.first == path) return it.second }
        val file = Path.of(path)
        if (!file.exists()) return null
        return runCatching { HuggingFaceTokenizer.newInstance(file, mapOf("truncation" to "false")) }.getOrNull()
            ?.also { loaded = path to it }
    }

    override fun tokenize(text: String): List<TokenizerResult> {
        val tokenizer = handle() ?: return emptyList()
        return tokenizer.encode(text, false, false).charTokenSpans.mapNotNull { span ->
            if (span == null || span.end <= span.start) null
            else TokenizerResult(text.substring(span.start, span.end), span.start, span.end - 1)
        }
    }

    override fun joinTokens(tokens: List<String>): String = tokens.joinToString("")

    override fun copy() = HuggingFaceFileTokenizer(path)
}
