package org.simbrain.network.llm

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import java.nio.file.Path

/**
 * Wraps a HuggingFace `tokenizer.json` (via DJL's standalone JNI binding to the Rust tokenizers
 * library, the same implementation Python uses, so ids match exactly). With [addSpecials] on,
 * special tokens are added per the file's post-processor (LFM2 prepends BOS); turn it off when
 * appending mid-stream text whose scaffolding is already in place. Literal special-token text
 * ("<|im_start|>") encodes to its single id either way.
 */
class LlmTokenizer(path: Path) : AutoCloseable {

    private val tokenizer = HuggingFaceTokenizer.newInstance(path, mapOf("truncation" to "false"))

    fun encode(text: String, addSpecials: Boolean = true): IntArray {
        val ids = tokenizer.encode(text, addSpecials, false).ids
        return IntArray(ids.size) { ids[it].toInt() }
    }

    fun decode(ids: IntArray, skipSpecials: Boolean = false): String =
        tokenizer.decode(LongArray(ids.size) { ids[it].toLong() }, skipSpecials)

    override fun close() = tokenizer.close()
}
