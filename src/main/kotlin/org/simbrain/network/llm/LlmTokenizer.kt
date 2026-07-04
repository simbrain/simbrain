package org.simbrain.network.llm

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import java.nio.file.Path

/**
 * Wraps a HuggingFace `tokenizer.json` (via DJL's standalone JNI binding to the Rust tokenizers
 * library, the same implementation Python uses, so ids match exactly). Special tokens are added
 * per the file's post-processor (LFM2 prepends BOS).
 */
class LlmTokenizer(path: Path) : AutoCloseable {

    private val tokenizer = HuggingFaceTokenizer.newInstance(path)

    fun encode(text: String): IntArray {
        val ids = tokenizer.encode(text).ids
        return IntArray(ids.size) { ids[it].toInt() }
    }

    fun decode(ids: IntArray): String = tokenizer.decode(LongArray(ids.size) { ids[it].toLong() })

    override fun close() = tokenizer.close()
}
