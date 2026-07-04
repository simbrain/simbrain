package org.simbrain.network.tensor

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.exp
import kotlin.random.Random

/**
 * M1 decision benchmark: elementwise kernel speed over heap FloatArray vs direct FloatBuffer,
 * at LFM2 activation sizes. Decides whether activations live off-heap next to the weights
 * (uniform storage, zero-copy BLAS) or stay on-heap and cross through staging buffers.
 * Also measures the staging copy itself (FloatArray -> off-heap and back).
 */

private fun bench(label: String, reps: Int, warmup: Int, run: () -> Unit): Double {
    repeat(warmup) { run() }
    val times = LongArray(reps)
    for (i in 0 until reps) {
        val t0 = System.nanoTime()
        run()
        times[i] = System.nanoTime() - t0
    }
    times.sort()
    val median = times[reps / 2].toDouble()
    println("| $label | ${"%.3f".format(median / 1e3)} µs |")
    return median
}

fun main() {
    println("JVM: ${System.getProperty("java.vendor")} ${System.getProperty("java.version")}")
    val rng = Random(7)
    val sizes = listOf(2560, 65536)

    for (n in sizes) {
        val srcArr = FloatArray(n) { rng.nextFloat() - 0.5f }
        val dstArr = FloatArray(n)
        val srcBuf: FloatBuffer = ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        val dstBuf: FloatBuffer = ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (i in 0 until n) srcBuf.put(i, srcArr[i])

        println()
        println("n=$n")
        println("| kernel | median |")
        println("|---|---|")

        val reps = if (n <= 4096) 20000 else 2000
        bench("silu array", reps, reps / 4) {
            for (i in 0 until n) {
                val x = srcArr[i]
                dstArr[i] = x / (1f + exp(-x))
            }
        }
        bench("silu directBuffer", reps, reps / 4) {
            for (i in 0 until n) {
                val x = srcBuf.get(i)
                dstBuf.put(i, x / (1f + exp(-x)))
            }
        }
        bench("mul-gate array", reps, reps / 4) {
            for (i in 0 until n) {
                dstArr[i] = srcArr[i] * dstArr[i]
            }
        }
        bench("mul-gate directBuffer", reps, reps / 4) {
            for (i in 0 until n) {
                dstBuf.put(i, srcBuf.get(i) * dstBuf.get(i))
            }
        }
        bench("rmsnorm-reduce array", reps, reps / 4) {
            var acc = 0f
            for (i in 0 until n) {
                val x = srcArr[i]
                acc += x * x
            }
            dstArr[0] = acc
        }
        bench("rmsnorm-reduce directBuffer", reps, reps / 4) {
            var acc = 0f
            for (i in 0 until n) {
                val x = srcBuf.get(i)
                acc += x * x
            }
            dstBuf.put(0, acc)
        }
        bench("staging copy array->buffer", reps, reps / 4) {
            srcBuf.put(0, srcArr[0]); dstBuf.position(0)
            dstBuf.put(srcArr, 0, n); dstBuf.position(0)
        }
        bench("staging copy buffer->array", reps, reps / 4) {
            srcBuf.position(0)
            srcBuf.get(dstArr, 0, n); srcBuf.position(0)
        }
    }
}
