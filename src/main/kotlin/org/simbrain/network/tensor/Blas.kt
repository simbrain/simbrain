package org.simbrain.network.tensor

import org.bytedeco.openblas.presets.openblas_nolapack

/**
 * Thread control for the OpenBLAS pool. This is a global setting on the native library, which
 * Smile shares (the build swaps Smile's bundled natives for the bytedeco artifact) — changing
 * it affects classic-network matmuls too. Set explicitly at startup; pin in parity harnesses.
 */
object Blas {

    var numThreads: Int
        get() = openblas_nolapack.blas_get_num_threads()
        set(value) = openblas_nolapack.blas_set_num_threads(value)

    inline fun <T> withThreads(n: Int, block: () -> T): T {
        val prev = numThreads
        numThreads = n
        try {
            return block()
        } finally {
            numThreads = prev
        }
    }
}
