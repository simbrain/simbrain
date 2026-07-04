package org.simbrain.network.tensor.op

import org.simbrain.network.tensor.FloatTensor

/**
 * A named handle to a tensor in an [OpPlan]. Ports are the hook points: probes, the renderer,
 * and interventions address tensors by stable port name, never by object identity.
 */
class TensorPort(val name: String, val tensor: FloatTensor) {
    override fun toString() = "TensorPort($name, ${tensor.rows}x${tensor.cols})"
}

/**
 * One step of a block's forward pass: reads [inputs], writes [outputs]. Ops are prebound to
 * their tensors at plan-build time, so [forward] runs allocation-free on preallocated
 * workspaces. An op that implements its VJP overrides [backward] and sets [hasBackward];
 * a plan can only record a tape if every op does.
 */
abstract class TensorOp(val name: String) {

    abstract val inputs: List<TensorPort>
    abstract val outputs: List<TensorPort>

    abstract fun forward()

    open val hasBackward: Boolean get() = false

    /** Tensors [backward] reads; the tape validates their versions at backward time. */
    open val savedForBackward: List<FloatTensor> get() = emptyList()

    /**
     * The op's VJP: reads the gradient of its outputs from [grads] and accumulates (+=) into
     * the gradients of its inputs. Must accumulate, never overwrite — fan-out sums naturally.
     */
    open fun backward(grads: Gradients): Unit = error("Op $name has no backward")

    override fun toString() =
        "$name(${inputs.joinToString { it.name }} -> ${outputs.joinToString { it.name }})"
}
