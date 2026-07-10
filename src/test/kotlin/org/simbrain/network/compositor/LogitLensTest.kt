package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.op.TensorPort
import kotlin.math.exp
import kotlin.math.sqrt

class LogitLensTest {

    @Test
    fun `lens reads off the softmax argmax of the normed projection`() {
        val embed = FloatTensor.of(4, 3, floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f,
            0.5f, 0.5f, 0.5f,
        ))
        val normWeight = FloatTensor.of(1, 3, floatArrayOf(1f, 2f, 1f))
        val eps = 1e-5f
        val resid = TensorPort("resid", FloatTensor.of(1, 3, floatArrayOf(0.2f, 0.8f, -0.4f)))
        val lens = LogitLens(embed, normWeight, eps, listOf(resid))

        lens.refresh()

        val x = floatArrayOf(0.2f, 0.8f, -0.4f)
        val inv = 1f / sqrt(x.map { it * it }.sum() / 3 + eps)
        val normed = floatArrayOf(x[0] * inv * 1f, x[1] * inv * 2f, x[2] * inv * 1f)
        val logits = FloatArray(4) { row ->
            (0 until 3).map { embed[row, it] * normed[it] }.sum()
        }
        val best = logits.indices.maxBy { logits[it] }
        val prob = 1f / logits.sumOf { exp((it - logits[best]).toDouble()) }.toFloat()

        assertEquals(best, lens.readings[0].tokenId)
        assertEquals(prob, lens.readings[0].prob, 1e-5f)
        assertEquals(1, best, "sanity: the boosted middle dimension should win")
    }

    @Test
    fun `async lens computes the same readings off the calling thread`() {
        val embed = FloatTensor.of(4, 3, floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f,
            0.5f, 0.5f, 0.5f,
        ))
        val normWeight = FloatTensor.of(1, 3, floatArrayOf(1f, 2f, 1f))
        val resid = TensorPort("resid", FloatTensor.of(1, 3, floatArrayOf(0.2f, 0.8f, -0.4f)))

        val sync = LogitLens(embed, normWeight, 1e-5f, listOf(resid))
        sync.refresh()

        val lens = LogitLens(embed, normWeight, 1e-5f, listOf(resid))
        lens.async = true
        val landed = java.util.concurrent.CountDownLatch(1)
        lens.onReadingsUpdated = { landed.countDown() }
        lens.refresh()
        // The source may be overwritten right after refresh returns; the snapshot must protect the pass.
        resid.tensor.copyFrom(floatArrayOf(9f, 9f, 9f))
        assertEquals(true, landed.await(5, java.util.concurrent.TimeUnit.SECONDS), "worker never landed")

        assertEquals(sync.readings[0].tokenId, lens.readings[0].tokenId)
        assertEquals(sync.readings[0].prob, lens.readings[0].prob, 1e-6f)
    }

    @Test
    fun `lens skips sources whose tensor has not changed`() {
        val embed = FloatTensor.of(2, 2, floatArrayOf(1f, 0f, 0f, 1f))
        val normWeight = FloatTensor.of(1, 2, floatArrayOf(1f, 1f))
        val resid = TensorPort("resid", FloatTensor.of(1, 2, floatArrayOf(1f, 0f)))
        val lens = LogitLens(embed, normWeight, 1e-5f, listOf(resid))

        lens.refresh()
        assertEquals(0, lens.readings[0].tokenId)

        resid.tensor.copyFrom(floatArrayOf(0f, 1f))
        lens.refresh()
        assertEquals(1, lens.readings[0].tokenId, "changed tensor must be re-read")
    }
}
