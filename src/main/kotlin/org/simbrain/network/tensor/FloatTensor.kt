package org.simbrain.network.tensor

/**
 * Minimal 2-D float tensor backing the LLM / new transformer stack. Row-major flat [data],
 * matching CblasRowMajor, so BLAS calls and renderer reads share the same buffer with no
 * layout conversion. Not used by the classic Smile-based network; conversion to DoubleArray
 * happens at the probe/coupling boundary.
 *
 * Mutation contract: ops write through destination-passing functions (see FloatTensorOps.kt)
 * and bump [version] once per mutating op. Hot loops may write [data] directly but must call
 * [markMutated] when done. The version counter feeds tape validation at backward time,
 * renderer dirty detection, and debug single-writer assertions.
 */
class FloatTensor(val rows: Int, val cols: Int, val data: FloatArray, val role: TensorRole = TensorRole.WORKSPACE) {

    constructor(rows: Int, cols: Int, role: TensorRole = TensorRole.WORKSPACE) :
            this(rows, cols, FloatArray(rows * cols), role)

    init {
        require(rows > 0 && cols > 0) { "Invalid shape ${rows}x$cols" }
        require(data.size == rows * cols) { "Data size ${data.size} does not match shape ${rows}x$cols" }
    }

    var version = 0L
        private set

    val size get() = data.size

    fun markMutated() {
        check(role != TensorRole.SNAPSHOT) { "Snapshot tensors are immutable after creation" }
        version++
    }

    operator fun get(r: Int, c: Int) = data[r * cols + c]

    operator fun set(r: Int, c: Int, value: Float) {
        data[r * cols + c] = value
        markMutated()
    }

    fun fill(value: Float) {
        data.fill(value)
        markMutated()
    }

    fun copyFrom(src: FloatTensor) {
        require(src.size == size) { "Size mismatch: ${src.rows}x${src.cols} into ${rows}x$cols" }
        src.data.copyInto(data)
        markMutated()
    }

    /** Immutable copy for tape saves and harvest boundaries (snapshot-on-record). */
    fun snapshot() = FloatTensor(rows, cols, data.copyOf(), TensorRole.SNAPSHOT)

    /** Same data, new shape. No copy; the returned tensor aliases this one's buffer. */
    fun reshaped(newRows: Int, newCols: Int): FloatTensor {
        require(newRows * newCols == size) { "Cannot reshape ${rows}x$cols to ${newRows}x$newCols" }
        return FloatTensor(newRows, newCols, data, role)
    }

    override fun toString() = "FloatTensor(${rows}x$cols, role=$role, version=$version)"

    companion object {
        fun vector(n: Int, role: TensorRole = TensorRole.WORKSPACE) = FloatTensor(n, 1, role)
    }
}

/**
 * Parameters are written only by weight load and (teaching blocks) the optimizer, and are keyed
 * by stable string names, not object identity. Workspaces are preallocated per-plan buffers
 * written by exactly one op per step. Snapshots are immutable after creation.
 */
enum class TensorRole { PARAMETER, WORKSPACE, SNAPSHOT }
