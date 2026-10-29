package org.simbrain.network.tensor

import org.bytedeco.javacpp.FloatPointer
import java.nio.FloatBuffer

/**
 * Minimal 2-D float tensor backing the LLM / new transformer stack. Storage is off-heap
 * ([pointer], row-major, matching CblasRowMajor): BLAS calls are zero-copy for weights and
 * activations alike (attention is activation-by-activation, so heap arrays would force staging
 * or GC-pinning there too), and the memory is never scanned or moved by GC. Kotlin kernels and
 * JVM readers use [data], a direct-buffer view of the same memory; heap copies happen only at
 * classic-Simbrain boundaries (probe harvest, renderer publish) via [toFloatArray].
 *
 * Mutation contract: ops write through destination-passing functions (see FloatTensorOps.kt)
 * and bump [version] once per mutating op. Hot loops may write [data] directly but must call
 * [markMutated] when done. The version counter feeds tape validation at backward time,
 * renderer dirty detection, and debug single-writer assertions.
 *
 * Workspace tensors are freed by GC (JavaCPP registers a deallocator); large parameter stores
 * should be freed deterministically via [deallocate] on model unload.
 */
class FloatTensor private constructor(
    val rows: Int,
    val cols: Int,
    val pointer: FloatPointer,
    val role: TensorRole
) {

    constructor(rows: Int, cols: Int, role: TensorRole = TensorRole.WORKSPACE) :
            this(rows, cols, FloatPointer((rows.toLong() * cols)), role)

    init {
        require(rows > 0 && cols > 0) { "Invalid shape ${rows}x$cols" }
    }

    val data: FloatBuffer = pointer.asBuffer()

    var version = 0L
        private set

    val size get() = rows * cols

    fun markMutated() {
        check(role != TensorRole.SNAPSHOT) { "Snapshot tensors are immutable after creation" }
        version++
    }

    operator fun get(r: Int, c: Int) = data.get(r * cols + c)

    operator fun set(r: Int, c: Int, value: Float) {
        data.put(r * cols + c, value)
        markMutated()
    }

    fun fill(value: Float) {
        for (i in 0 until size) data.put(i, value)
        markMutated()
    }

    fun copyFrom(src: FloatTensor) {
        require(src.size == size) { "Size mismatch: ${src.rows}x${src.cols} into ${rows}x$cols" }
        data.duplicate().put(src.data.duplicate())
        markMutated()
    }

    fun copyFrom(src: FloatArray) {
        require(src.size == size) { "Size mismatch: ${src.size} values into ${rows}x$cols" }
        data.duplicate().put(src)
        markMutated()
    }

    /** Heap copy for probe/coupling/renderer boundaries. */
    fun toFloatArray(): FloatArray {
        val out = FloatArray(size)
        data.duplicate().get(out)
        return out
    }

    /** Immutable copy for tape saves and harvest boundaries (snapshot-on-record). */
    fun snapshot(): FloatTensor {
        val copy = FloatTensor(rows, cols, FloatPointer(size.toLong()), TensorRole.SNAPSHOT)
        copy.data.duplicate().put(data.duplicate())
        return copy
    }

    /** Same memory, new shape. No copy; the returned tensor aliases this one's buffer. */
    fun reshaped(newRows: Int, newCols: Int): FloatTensor {
        require(newRows * newCols == size) { "Cannot reshape ${rows}x$cols to ${newRows}x$newCols" }
        return FloatTensor(newRows, newCols, FloatPointer(pointer), role)
    }

    /**
     * Frees the native memory immediately. Only for owned long-lived tensors (parameter stores
     * on model unload); any later access through this tensor or its views is invalid.
     */
    fun deallocate() = pointer.deallocate()

    override fun toString() = "FloatTensor(${rows}x$cols, role=$role, version=$version)"

    companion object {
        fun vector(n: Int, role: TensorRole = TensorRole.WORKSPACE) = FloatTensor(n, 1, role)

        fun of(rows: Int, cols: Int, values: FloatArray, role: TensorRole = TensorRole.WORKSPACE) =
            FloatTensor(rows, cols, role).apply { copyFrom(values) }
    }
}

/**
 * Parameters are written only by weight load and (teaching blocks) the optimizer, and are keyed
 * by stable string names, not object identity. Workspaces are preallocated per-plan buffers
 * written by exactly one op per step. Snapshots are immutable after creation.
 */
enum class TensorRole { PARAMETER, WORKSPACE, SNAPSHOT }
