package org.simbrain.network.tensor

import org.bytedeco.openblas.global.openblas_nolapack.CblasNoTrans
import org.bytedeco.openblas.global.openblas_nolapack.CblasRowMajor
import org.bytedeco.openblas.global.openblas_nolapack.cblas_saxpy
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sdot
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sgemm
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sgemv
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sscal

/**
 * Destination-passing ops over [FloatTensor], sgemm/sgemv-backed. Every op takes an explicit
 * `out` and supports BLAS alpha/beta fusion (`out = alpha * f(a, b) + beta * out`), so
 * accumulate-into-destination patterns need no temporaries. Allocating conveniences exist for
 * casual use; decode loops run on preallocated workspaces.
 */

/** out = alpha * (a x b) + beta * out */
fun matmul(a: FloatTensor, b: FloatTensor, out: FloatTensor, alpha: Float = 1f, beta: Float = 0f) {
    require(a.cols == b.rows) { "matmul inner dims: ${a.rows}x${a.cols} x ${b.rows}x${b.cols}" }
    require(out.rows == a.rows && out.cols == b.cols) {
        "matmul out ${out.rows}x${out.cols} != ${a.rows}x${b.cols}"
    }
    cblas_sgemm(
        CblasRowMajor, CblasNoTrans, CblasNoTrans,
        a.rows, b.cols, a.cols,
        alpha, a.data, a.cols, b.data, b.cols,
        beta, out.data, out.cols
    )
    out.markMutated()
}

infix fun FloatTensor.matmul(b: FloatTensor) = FloatTensor(rows, b.cols).also { matmul(this, b, it) }

/** out = alpha * (a . x) + beta * out, where x and out are vectors. */
fun matvec(a: FloatTensor, x: FloatTensor, out: FloatTensor, alpha: Float = 1f, beta: Float = 0f) {
    require(x.size == a.cols) { "matvec x size ${x.size} != ${a.cols}" }
    require(out.size == a.rows) { "matvec out size ${out.size} != ${a.rows}" }
    cblas_sgemv(
        CblasRowMajor, CblasNoTrans,
        a.rows, a.cols,
        alpha, a.data, a.cols, x.data, 1,
        beta, out.data, 1
    )
    out.markMutated()
}

infix fun FloatTensor.matvec(x: FloatTensor) = FloatTensor.vector(rows).also { matvec(this, x, it) }

/** y += alpha * x */
fun axpy(alpha: Float, x: FloatTensor, y: FloatTensor) {
    require(x.size == y.size) { "axpy size ${x.size} != ${y.size}" }
    cblas_saxpy(x.size, alpha, x.data, 1, y.data, 1)
    y.markMutated()
}

/** x *= alpha */
fun scal(alpha: Float, x: FloatTensor) {
    cblas_sscal(x.size, alpha, x.data, 1)
    x.markMutated()
}

fun dot(x: FloatTensor, y: FloatTensor): Float {
    require(x.size == y.size) { "dot size ${x.size} != ${y.size}" }
    return cblas_sdot(x.size, x.data, 1, y.data, 1)
}

/** out = a * b elementwise (gate ops). Plain loop; JIT-vectorized, no BLAS call overhead. */
fun hadamard(a: FloatTensor, b: FloatTensor, out: FloatTensor) {
    require(a.size == b.size && a.size == out.size) { "hadamard sizes ${a.size}, ${b.size}, ${out.size}" }
    val ad = a.data
    val bd = b.data
    val od = out.data
    for (i in od.indices) {
        od[i] = ad[i] * bd[i]
    }
    out.markMutated()
}

/**
 * Elementwise kernels as inline transforms: the lambda inlines into a plain loop, so this JITs
 * to vectorized code (unlike Smile's applyFunction, which megamorphizes).
 */
inline fun FloatTensor.transformInPlace(f: (Float) -> Float) {
    for (i in data.indices) {
        data[i] = f(data[i])
    }
    markMutated()
}

inline fun FloatTensor.transformInto(out: FloatTensor, f: (Float) -> Float) {
    require(size == out.size) { "transformInto size $size != ${out.size}" }
    val src = data
    val dst = out.data
    for (i in dst.indices) {
        dst[i] = f(src[i])
    }
    out.markMutated()
}
