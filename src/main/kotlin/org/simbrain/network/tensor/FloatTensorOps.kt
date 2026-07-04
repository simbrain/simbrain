package org.simbrain.network.tensor

import org.bytedeco.openblas.global.openblas_nolapack.CblasNoTrans
import org.bytedeco.openblas.global.openblas_nolapack.CblasRowMajor
import org.bytedeco.openblas.global.openblas_nolapack.CblasTrans
import org.bytedeco.openblas.global.openblas_nolapack.cblas_saxpy
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sdot
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sgemm
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sgemv
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sger
import org.bytedeco.openblas.global.openblas_nolapack.cblas_sscal

/**
 * Destination-passing ops over [FloatTensor], sgemm/sgemv-backed. Every op takes an explicit
 * `out` and supports BLAS alpha/beta fusion (`out = alpha * f(a, b) + beta * out`), so
 * accumulate-into-destination patterns need no temporaries. Allocating conveniences exist for
 * casual use; decode loops run on preallocated workspaces.
 */

/** out = alpha * (a x b) + beta * out. With [transposeA]/[transposeB], that side is used as its transpose. */
fun matmul(
    a: FloatTensor,
    b: FloatTensor,
    out: FloatTensor,
    alpha: Float = 1f,
    beta: Float = 0f,
    transposeB: Boolean = false,
    transposeA: Boolean = false
) {
    val aRows = if (transposeA) a.cols else a.rows
    val aCols = if (transposeA) a.rows else a.cols
    val bRows = if (transposeB) b.cols else b.rows
    val bCols = if (transposeB) b.rows else b.cols
    require(aCols == bRows) { "matmul inner dims: ${aRows}x$aCols x ${bRows}x$bCols" }
    require(out.rows == aRows && out.cols == bCols) {
        "matmul out ${out.rows}x${out.cols} != ${aRows}x$bCols"
    }
    cblas_sgemm(
        CblasRowMajor,
        if (transposeA) CblasTrans else CblasNoTrans,
        if (transposeB) CblasTrans else CblasNoTrans,
        aRows, bCols, aCols,
        alpha, a.pointer, a.cols, b.pointer, b.cols,
        beta, out.pointer, out.cols
    )
    out.markMutated()
}

infix fun FloatTensor.matmul(b: FloatTensor) = FloatTensor(rows, b.cols).also { matmul(this, b, it) }

/**
 * out = alpha * (a . x) + beta * out, where x and out are vectors. [rowCount] restricts to the
 * first rows of a (used for growing KV-cache slices); with [transposeA] computes a^T . x, so
 * x spans [rowCount] and out spans a.cols. Oversized x/out buffers are allowed for cache reuse.
 */
fun matvec(
    a: FloatTensor,
    x: FloatTensor,
    out: FloatTensor,
    alpha: Float = 1f,
    beta: Float = 0f,
    rowCount: Int = a.rows,
    transposeA: Boolean = false
) {
    require(rowCount in 1..a.rows) { "matvec rowCount $rowCount out of 1..${a.rows}" }
    if (transposeA) {
        require(x.size >= rowCount) { "matvec x size ${x.size} < rowCount $rowCount" }
        require(out.size >= a.cols) { "matvec out size ${out.size} < ${a.cols}" }
    } else {
        require(x.size >= a.cols) { "matvec x size ${x.size} < ${a.cols}" }
        require(out.size >= rowCount) { "matvec out size ${out.size} < rowCount $rowCount" }
    }
    cblas_sgemv(
        CblasRowMajor, if (transposeA) CblasTrans else CblasNoTrans,
        rowCount, a.cols,
        alpha, a.pointer, a.cols, x.pointer, 1,
        beta, out.pointer, 1
    )
    out.markMutated()
}

infix fun FloatTensor.matvec(x: FloatTensor) = FloatTensor.vector(rows).also { matvec(this, x, it) }

/** y += alpha * x */
fun axpy(alpha: Float, x: FloatTensor, y: FloatTensor) {
    require(x.size == y.size) { "axpy size ${x.size} != ${y.size}" }
    cblas_saxpy(x.size, alpha, x.pointer, 1, y.pointer, 1)
    y.markMutated()
}

/** x *= alpha */
fun scal(alpha: Float, x: FloatTensor) {
    cblas_sscal(x.size, alpha, x.pointer, 1)
    x.markMutated()
}

/** a += alpha * (x outer y), the rank-1 update backing linear-layer weight gradients. */
fun ger(x: FloatTensor, y: FloatTensor, a: FloatTensor, alpha: Float = 1f) {
    require(a.rows == x.size && a.cols == y.size) {
        "ger ${a.rows}x${a.cols} != ${x.size} outer ${y.size}"
    }
    cblas_sger(CblasRowMajor, a.rows, a.cols, alpha, x.pointer, 1, y.pointer, 1, a.pointer, a.cols)
    a.markMutated()
}

fun dot(x: FloatTensor, y: FloatTensor): Float {
    require(x.size == y.size) { "dot size ${x.size} != ${y.size}" }
    return cblas_sdot(x.size, x.pointer, 1, y.pointer, 1)
}

/** out = a * b elementwise (gate ops). Plain loop over the direct-buffer views. */
fun hadamard(a: FloatTensor, b: FloatTensor, out: FloatTensor) {
    require(a.size == b.size && a.size == out.size) { "hadamard sizes ${a.size}, ${b.size}, ${out.size}" }
    val ad = a.data
    val bd = b.data
    val od = out.data
    for (i in 0 until out.size) {
        od.put(i, ad.get(i) * bd.get(i))
    }
    out.markMutated()
}

/**
 * Elementwise kernels as inline transforms: the lambda inlines into a plain loop over the
 * direct-buffer view, so this JITs to straight loads/stores (unlike Smile's applyFunction,
 * which megamorphizes).
 */
inline fun FloatTensor.transformInPlace(f: (Float) -> Float) {
    for (i in 0 until size) {
        data.put(i, f(data.get(i)))
    }
    markMutated()
}

inline fun FloatTensor.transformInto(out: FloatTensor, f: (Float) -> Float) {
    require(size == out.size) { "transformInto size $size != ${out.size}" }
    val src = data
    val dst = out.data
    for (i in 0 until size) {
        dst.put(i, f(src.get(i)))
    }
    out.markMutated()
}
