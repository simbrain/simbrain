package org.simbrain.network.llm

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.FloatTensor
import java.util.Random

class Lfm2RewindTest {

    private fun tinyConfig() = Lfm2Config(
        hiddenSize = 16,
        numLayers = 4,
        attentionLayers = setOf(2),
        numHeads = 4,
        numKvHeads = 2,
        headDim = 4,
        intermediateSize = 24,
        vocabSize = 32,
        maxSeqLen = 24,
    )

    private fun syntheticParams(config: Lfm2Config): Map<String, FloatTensor> {
        val random = Random(42L)
        fun t(rows: Int, cols: Int) = FloatTensor(rows, cols).apply {
            for (i in 0 until size) data.put(i, (random.nextFloat() - 0.5f) * 0.4f)
        }
        val params = HashMap<String, FloatTensor>()
        params["model.embed_tokens.weight"] = t(config.vocabSize, config.hiddenSize)
        params["model.embedding_norm.weight"] = t(1, config.hiddenSize)
        for (i in 0 until config.numLayers) {
            val p = "model.layers.$i"
            params["$p.operator_norm.weight"] = t(1, config.hiddenSize)
            params["$p.ffn_norm.weight"] = t(1, config.hiddenSize)
            params["$p.feed_forward.w1.weight"] = t(config.intermediateSize, config.hiddenSize)
            params["$p.feed_forward.w3.weight"] = t(config.intermediateSize, config.hiddenSize)
            params["$p.feed_forward.w2.weight"] = t(config.hiddenSize, config.intermediateSize)
            if (i in config.attentionLayers) {
                params["$p.self_attn.q_proj.weight"] = t(config.numHeads * config.headDim, config.hiddenSize)
                params["$p.self_attn.k_proj.weight"] = t(config.kvDim, config.hiddenSize)
                params["$p.self_attn.v_proj.weight"] = t(config.kvDim, config.hiddenSize)
                params["$p.self_attn.out_proj.weight"] = t(config.hiddenSize, config.numHeads * config.headDim)
                params["$p.self_attn.q_layernorm.weight"] = t(1, config.headDim)
                params["$p.self_attn.k_layernorm.weight"] = t(1, config.headDim)
            } else {
                params["$p.conv.in_proj.weight"] = t(3 * config.hiddenSize, config.hiddenSize)
                params["$p.conv.out_proj.weight"] = t(config.hiddenSize, config.hiddenSize)
                params["$p.conv.conv.weight"] = t(config.hiddenSize, config.convKernel)
            }
        }
        return params
    }

    private fun FloatTensor.copyValues() = FloatArray(size) { data.get(it) }

    @Test
    fun `rewinding to a conv snapshot reproduces from-scratch logits bit for bit`() {
        val config = tinyConfig()
        val params = syntheticParams(config)
        val model = Lfm2Model(config, params)

        val prefix = listOf(1, 5, 9, 13, 2, 7, 11, 3)
        val checkpointAt = 5
        var snapshot: List<FloatArray>? = null
        prefix.forEach { id ->
            model.forwardToken(id)
            if (model.position == checkpointAt) snapshot = model.snapshotConvState()
        }
        listOf(4, 6, 8).forEach { model.forwardToken(it) }

        model.rewindTo(checkpointAt, snapshot!!)
        assertEquals(checkpointAt, model.position)
        val replayTail = prefix.subList(checkpointAt, prefix.size) + listOf(10, 12)
        val rewound = replayTail.map { model.forwardToken(it).copyValues() }

        val fresh = Lfm2Model(config, params)
        val expected = (prefix + listOf(10, 12)).map { fresh.forwardToken(it).copyValues() }

        rewound.forEachIndexed { i, logits ->
            assertArrayEquals(expected[checkpointAt + i], logits,
                "logits diverge at replay step $i (position ${checkpointAt + i})")
        }
    }

    @Test
    fun `rewinding to position zero with a fresh snapshot matches a reset`() {
        val config = tinyConfig()
        val params = syntheticParams(config)
        val model = Lfm2Model(config, params)
        val zeroSnapshot = model.snapshotConvState()

        val stream = listOf(3, 8, 14, 6)
        stream.forEach { model.forwardToken(it) }
        model.rewindTo(0, zeroSnapshot)
        val replayed = stream.map { model.forwardToken(it).copyValues() }

        val fresh = Lfm2Model(config, params)
        val expected = stream.map { fresh.forwardToken(it).copyValues() }
        replayed.forEachIndexed { i, logits ->
            assertArrayEquals(expected[i], logits, "logits diverge at step $i")
        }
    }
}
