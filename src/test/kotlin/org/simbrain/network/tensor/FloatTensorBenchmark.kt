package org.simbrain.network.tensor

import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.openblas.global.openblas_nolapack.CblasNoTrans
import org.bytedeco.openblas.global.openblas_nolapack.CblasRowMajor
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sgemm
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sgemv
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * M0 benchmark spike: raw cblas call-convention comparison (heap float[] vs direct FloatBuffer
 * vs off-heap FloatPointer) across LFM2.5-230M decode/prefill shapes and OpenBLAS thread counts,
 * plus a GCLocker jank probe (float[] overloads pin via GetPrimitiveArrayCritical, which can
 * stall allocation on other threads for the GEMV's duration).
 *
 * Run standalone after compileTestKotlin:
 *   java -cp <main+test classes>:<kotlin-stdlib>:<javacpp+openblas jars incl. platform natives> \
 *     org.simbrain.network.tensor.FloatTensorBenchmarkKt
 */

sealed class Storage(val size: Int) {
    abstract fun free()

    class HeapArray(size: Int, rng: Random) : Storage(size) {
        val data = FloatArray(size) { rng.nextFloat() - 0.5f }
        override fun free() {}
    }

    class DirectBuffer(size: Int, rng: Random) : Storage(size) {
        val data: FloatBuffer = ByteBuffer.allocateDirect(size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        init {
            for (i in 0 until size) data.put(i, rng.nextFloat() - 0.5f)
        }
        override fun free() {}
    }

    class OffHeapPointer(size: Int, rng: Random) : Storage(size) {
        val data = FloatPointer(size.toLong())
        init {
            val chunk = FloatArray(minOf(size, 1 shl 20))
            var offset = 0L
            while (offset < size) {
                val n = minOf(chunk.size.toLong(), size - offset).toInt()
                for (i in 0 until n) chunk[i] = rng.nextFloat() - 0.5f
                data.position(offset).put(chunk, 0, n)
                offset += n
            }
            data.position(0L)
        }
        override fun free() = data.deallocate()
    }
}

enum class Convention(val label: String) {
    ARRAY("float[]"), BUFFER("DirectFloatBuffer"), POINTER("FloatPointer");

    fun alloc(size: Int, rng: Random): Storage = when (this) {
        ARRAY -> Storage.HeapArray(size, rng)
        BUFFER -> Storage.DirectBuffer(size, rng)
        POINTER -> Storage.OffHeapPointer(size, rng)
    }
}

fun gemv(m: Int, n: Int, a: Storage, x: Storage, y: Storage) {
    when (a) {
        is Storage.HeapArray -> cblas_sgemv(
            CblasRowMajor, CblasNoTrans, m, n, 1f, a.data, n,
            (x as Storage.HeapArray).data, 1, 0f, (y as Storage.HeapArray).data, 1
        )
        is Storage.DirectBuffer -> cblas_sgemv(
            CblasRowMajor, CblasNoTrans, m, n, 1f, a.data, n,
            (x as Storage.DirectBuffer).data, 1, 0f, (y as Storage.DirectBuffer).data, 1
        )
        is Storage.OffHeapPointer -> cblas_sgemv(
            CblasRowMajor, CblasNoTrans, m, n, 1f, a.data, n,
            (x as Storage.OffHeapPointer).data, 1, 0f, (y as Storage.OffHeapPointer).data, 1
        )
    }
}

fun gemm(m: Int, n: Int, k: Int, a: Storage, b: Storage, c: Storage) {
    when (a) {
        is Storage.HeapArray -> cblas_sgemm(
            CblasRowMajor, CblasNoTrans, CblasNoTrans, m, n, k, 1f, a.data, k,
            (b as Storage.HeapArray).data, n, 0f, (c as Storage.HeapArray).data, n
        )
        is Storage.DirectBuffer -> cblas_sgemm(
            CblasRowMajor, CblasNoTrans, CblasNoTrans, m, n, k, 1f, a.data, k,
            (b as Storage.DirectBuffer).data, n, 0f, (c as Storage.DirectBuffer).data, n
        )
        is Storage.OffHeapPointer -> cblas_sgemm(
            CblasRowMajor, CblasNoTrans, CblasNoTrans, m, n, k, 1f, a.data, k,
            (b as Storage.OffHeapPointer).data, n, 0f, (c as Storage.OffHeapPointer).data, n
        )
    }
}

data class Shape(val label: String, val m: Int, val n: Int, val k: Int) {
    val isGemv get() = k == 1
    val flops get() = 2.0 * m * n * maxOf(k, 1)
}

val SHAPES = listOf(
    Shape("logits GEMV 65536x1024", 65536, 1024, 1),
    Shape("SwiGLU GEMV 2560x1024", 2560, 1024, 1),
    Shape("proj GEMV 1024x1024", 1024, 1024, 1),
    Shape("prefill GEMM 2560x1024 seq64", 2560, 64, 1024),
    Shape("prefill GEMM 2560x1024 seq256", 2560, 256, 1024),
)

fun benchCell(shape: Shape, convention: Convention): DoubleArray {
    val rng = Random(1234)
    val a = convention.alloc(if (shape.isGemv) shape.m * shape.n else shape.m * shape.k, rng)
    val b = convention.alloc(if (shape.isGemv) shape.n else shape.k * shape.n, rng)
    val c = convention.alloc(if (shape.isGemv) shape.m else shape.m * shape.n, rng)
    val run: () -> Unit = if (shape.isGemv) {
        { gemv(shape.m, shape.n, a, b, c) }
    } else {
        { gemm(shape.m, shape.n, shape.k, a, b, c) }
    }
    val warmupEnd = System.nanoTime() + 400_000_000L
    while (System.nanoTime() < warmupEnd) run()
    val times = ArrayList<Long>(2048)
    val measureEnd = System.nanoTime() + 1_200_000_000L
    while (System.nanoTime() < measureEnd || times.size < 20) {
        val t0 = System.nanoTime()
        run()
        times.add(System.nanoTime() - t0)
        if (times.size >= 20000) break
    }
    a.free(); b.free(); c.free()
    times.sort()
    val median = times[times.size / 2].toDouble()
    val p95 = times[(times.size * 95) / 100].toDouble()
    return doubleArrayOf(median, p95, times.size.toDouble())
}

fun jankProbe(convention: Convention?, durationMs: Long = 4000): Triple<Long, Int, Long> {
    val stop = AtomicBoolean(false)
    var maxStallNs = 0L
    var stallsOver10Ms = 0
    var iterations = 0L
    val window = arrayOfNulls<ByteArray>(64)
    val allocator = Thread {
        var i = 0
        var last = System.nanoTime()
        while (!stop.get()) {
            window[i % window.size] = ByteArray(1 shl 16)
            i++
            val now = System.nanoTime()
            val gap = now - last
            if (gap > maxStallNs) maxStallNs = gap
            if (gap > 10_000_000L) stallsOver10Ms++
            last = now
            iterations++
        }
    }
    val rng = Random(99)
    val m = 65536
    val n = 1024
    val a = convention?.alloc(m * n, rng)
    val x = convention?.alloc(n, rng)
    val y = convention?.alloc(m, rng)
    if (convention != null) {
        repeat(20) { gemv(m, n, a!!, x!!, y!!) }
    }
    allocator.start()
    val end = System.nanoTime() + durationMs * 1_000_000L
    while (System.nanoTime() < end) {
        if (convention != null) gemv(m, n, a!!, x!!, y!!) else Thread.onSpinWait()
    }
    stop.set(true)
    allocator.join()
    a?.free(); x?.free(); y?.free()
    return Triple(maxStallNs, stallsOver10Ms, iterations)
}

fun main() {
    val runtime = Runtime.getRuntime()
    println("JVM: ${System.getProperty("java.vendor")} ${System.getProperty("java.version")}")
    println("Cores: ${runtime.availableProcessors()}, maxHeap: ${runtime.maxMemory() / (1 shl 20)} MB")
    println("OpenBLAS default threads: ${Blas.numThreads}")
    println()

    val threadCounts = listOf(1, 4, 8)
    println("| shape | convention | threads | median ms | p95 ms | GFLOP/s | reps |")
    println("|---|---|---|---|---|---|---|")
    for (shape in SHAPES) {
        for (convention in Convention.entries) {
            for (t in threadCounts) {
                Blas.numThreads = t
                val (median, p95, reps) = benchCell(shape, convention).let { Triple(it[0], it[1], it[2]) }
                val gflops = shape.flops / median
                println(
                    "| ${shape.label} | ${convention.label} | $t | ${"%.3f".format(median / 1e6)} " +
                            "| ${"%.3f".format(p95 / 1e6)} | ${"%.2f".format(gflops)} | ${reps.toInt()} |"
                )
            }
        }
    }

    println()
    println("GCLocker jank probe (allocation-thread stalls while logits GEMV runs, 4s each):")
    Blas.numThreads = 1
    println("| convention | max stall ms | stalls >10ms | alloc iterations |")
    println("|---|---|---|---|")
    for (convention in listOf<Convention?>(null) + Convention.entries) {
        val (maxStall, stalls, iters) = jankProbe(convention)
        val label = convention?.label ?: "baseline (no GEMV)"
        println("| $label | ${"%.2f".format(maxStall / 1e6)} | $stalls | $iters |")
    }
}
