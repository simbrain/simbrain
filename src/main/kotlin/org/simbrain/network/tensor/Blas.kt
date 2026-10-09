/**
 * Thread control for the OpenBLAS pool shared by every matrix library in Simbrain (Smile's natives are swapped for
 * the bytedeco artifact, so classic networks, CNN dense layers, and tensor models all use the same pool).
 */
package org.simbrain.network.tensor

import org.bytedeco.openblas.presets.openblas_nolapack
import kotlin.concurrent.thread

/**
 * The thread count is a global setting on the native library. OpenBLAS defaults to every logical CPU, which is
 * several times slower than a few threads for the small matrices Simbrain multiplies: threads are handed work too
 * small to repay the hand-off, hyperthreads and efficiency cores take an equal share of a statically split product,
 * and the slowest thread sets the pace. [ensureDefaultThreads] replaces that default with [defaultThreads] unless the
 * user chose a count with an OpenBLAS environment variable or code already set [numThreads] explicitly.
 */
object Blas {

    /**
     * Four threads was the fastest setting, or within a few percent of it, for tiny language model training on both
     * an 8+2 core Apple M1 Pro and a 16-core, 32-thread AMD Ryzen AI Max+ 395, where the library default was 2-3x
     * slower.
     */
    val defaultThreads = minOf(4, Runtime.getRuntime().availableProcessors())

    private val threadEnvironmentVariables = listOf("OPENBLAS_NUM_THREADS", "GOTO_NUM_THREADS", "OMP_NUM_THREADS")

    @Volatile
    private var configured = false

    var numThreads: Int
        get() = openblas_nolapack.blas_get_num_threads()
        set(value) = synchronized(this) {
            configured = true
            setNative(value)
        }

    // The native setter resolves its backend symbol on every call (up to ~0.2 s on macOS), so skip no-op sets.
    private fun setNative(value: Int) {
        if (value != openblas_nolapack.blas_get_num_threads()) openblas_nolapack.blas_set_num_threads(value)
    }

    /**
     * Applies [defaultThreads] once, before the first matrix work. Safe to call from any thread and cheap after the
     * first call; a call that arrives while another thread is applying the default waits for it to finish.
     */
    fun ensureDefaultThreads() {
        if (configured) return
        synchronized(this) {
            if (configured) return
            if (threadEnvironmentVariables.none { System.getenv(it) != null }) setNative(defaultThreads)
            configured = true
        }
    }

    /**
     * Starts [ensureDefaultThreads] on a background thread, so the first matrix work does not wait for the native
     * setter. Does nothing once the thread count is configured.
     */
    fun ensureDefaultThreadsInBackground() {
        if (!configured) thread(isDaemon = true, name = "OpenBLAS thread setup") { ensureDefaultThreads() }
    }

    inline fun <T> withThreads(n: Int, block: () -> T): T {
        ensureDefaultThreads()
        val prev = numThreads
        numThreads = n
        try {
            return block()
        } finally {
            numThreads = prev
        }
    }
}
