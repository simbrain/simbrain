package org.simbrain.network.tensor.op

/**
 * A deterministic schedule of [TensorOp]s — the forward pass of one block, run at token or op
 * granularity. The plan owns the port registry (every port has one statically known writer op),
 * bumps tensor versions as ops complete (renderer dirty tracking), and fires port hooks the
 * moment a value is produced (probe bridge). Not a dynamic graph: the op list is fixed at
 * build time.
 */
class OpPlan(val ops: List<TensorOp>) {

    val ports: Map<String, TensorPort>

    private val writers = HashMap<String, TensorOp>()
    private val hooksByOp = HashMap<TensorOp, MutableList<Pair<TensorPort, (TensorPort) -> Unit>>>()

    init {
        require(ops.isNotEmpty()) { "Empty op plan" }
        val byName = HashMap<String, TensorPort>()
        for (op in ops) {
            for (port in op.outputs) {
                writers.put(port.name, op)?.let { other ->
                    throw IllegalArgumentException(
                        "Port ${port.name} written by both ${other.name} and ${op.name}"
                    )
                }
            }
            for (port in op.inputs + op.outputs) {
                val existing = byName.putIfAbsent(port.name, port)
                require(existing == null || existing === port) {
                    "Two different ports share the name ${port.name}"
                }
            }
        }
        ports = byName
    }

    fun port(name: String) = ports[name] ?: error("No port named $name")

    /** True when every op has a VJP, i.e. a tape can be recorded through the whole plan. */
    val trainable get() = ops.all { it.hasBackward }

    /**
     * Registers [hook] to run right after the op that writes [name] completes, with the port's
     * freshly produced value. Harvest-style consumers copy ([FloatTensor.toFloatArray] or
     * [FloatTensor.snapshot]) if they keep the value past the current op.
     */
    fun onPort(name: String, hook: (TensorPort) -> Unit): HookHandle {
        val port = port(name)
        val writer = writers[name]
            ?: error("Port $name has no writer op in this plan; cannot hook it")
        val list = hooksByOp.getOrPut(writer) { mutableListOf() }
        val entry = port to hook
        list.add(entry)
        return HookHandle { list.remove(entry) }
    }

    /** Index of the next op to run; 0 means at a step boundary. */
    var cursor = 0
        private set

    /** Runs one full pass. Must be at a step boundary (not mid-way through micro-stepping). */
    fun forward(tape: Tape? = null) {
        check(cursor == 0) { "forward() called mid-step at op $cursor (${ops[cursor].name})" }
        repeat(ops.size) { stepOp(tape) }
    }

    /**
     * Runs the single next op (micro-stepping), fires its hooks, and returns it. Wraps back to
     * op 0 after the last op, completing the pass.
     */
    fun stepOp(tape: Tape? = null): TensorOp {
        val op = ops[cursor]
        if (tape != null) {
            for (input in op.inputs) {
                require(op.outputs.none { it.tensor === input.tensor }) {
                    "In-place op ${op.name} cannot be recorded (input ${input.name} aliases an output)"
                }
            }
        }
        op.forward()
        for (out in op.outputs) out.tensor.markMutated()
        tape?.record(op)
        hooksByOp[op]?.forEach { (port, hook) -> hook(port) }
        cursor = (cursor + 1) % ops.size
        return op
    }
}

class HookHandle internal constructor(private val removal: () -> Unit) {
    fun remove() = removal()
}
