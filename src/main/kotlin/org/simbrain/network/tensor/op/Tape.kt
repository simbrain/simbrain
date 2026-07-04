package org.simbrain.network.tensor.op

import org.simbrain.network.tensor.FloatTensor
import java.util.IdentityHashMap

/**
 * Gradient buffers keyed by forward-tensor identity: one zero-initialized, same-shape tensor
 * per forward tensor, allocated lazily on first touch and reused across training steps
 * (call [zeroAll] between steps). Parameter gradients are read out by the optimizer via the
 * parameter's port name at the call site; identity keying here keeps VJPs allocation-free.
 */
class Gradients {

    private val map = IdentityHashMap<FloatTensor, FloatTensor>()

    fun of(t: FloatTensor): FloatTensor =
        map.getOrPut(t) { FloatTensor(t.rows, t.cols).apply { fill(0f) } }

    fun zeroAll() = map.values.forEach { it.fill(0f) }
}

/**
 * Reverse-mode tape: records ops during a training-mode forward pass; [backward] walks them in
 * reverse, chaining VJPs. Saved tensors are kept by reference and validated by version at
 * backward time — with pure single-writer plans nothing is overwritten within a pass, so no
 * defensive copies are needed (snapshot explicitly where an op knows better).
 */
class Tape {

    private class Entry(val op: TensorOp, val saved: List<FloatTensor>, val versions: LongArray)

    private val entries = ArrayList<Entry>()

    val size get() = entries.size

    fun record(op: TensorOp) {
        require(op.hasBackward) {
            "Op ${op.name} has no backward; the plan is not trainable through it"
        }
        val saved = op.savedForBackward
        entries.add(Entry(op, saved, LongArray(saved.size) { saved[it].version }))
    }

    /**
     * Seeds d(loss)/d(loss) = 1 on the (scalar) [loss] port and backpropagates through every
     * recorded op in reverse order, accumulating into [grads].
     */
    fun backward(loss: TensorPort, grads: Gradients) {
        require(loss.tensor.size == 1) { "Loss port ${loss.name} is not scalar" }
        grads.of(loss.tensor).fill(1f)
        for (entry in entries.asReversed()) {
            for (i in entry.saved.indices) {
                check(entry.saved[i].version == entry.versions[i]) {
                    "Tensor saved by ${entry.op.name} was mutated between record and backward"
                }
            }
            entry.op.backward(grads)
        }
    }

    fun clear() = entries.clear()
}
