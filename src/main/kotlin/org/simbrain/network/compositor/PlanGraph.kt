package org.simbrain.network.compositor

import org.simbrain.network.tensor.op.OpPlan
import org.simbrain.network.tensor.op.TensorOp

/**
 * Port-level adjacency over an [OpPlan]: which op writes each port and which ops read it.
 * This is what makes compositor layout and trace derivable from the plan instead of
 * hand-coded — upstream/downstream reachability and tile-to-tile edges all fall out of
 * the op list.
 */
class PlanGraph(val plan: OpPlan) {

    private val writerOf = HashMap<String, TensorOp>()
    private val readersOf = HashMap<String, MutableList<TensorOp>>()

    init {
        for (op in plan.ops) {
            for (port in op.outputs) writerOf[port.name] = op
            for (port in op.inputs) readersOf.getOrPut(port.name) { mutableListOf() }.add(op)
        }
    }

    /** All ports whose values (transitively) feed [name], not including [name] itself. */
    fun upstreamPorts(name: String): Set<String> {
        val visited = HashSet<String>()
        val stack = ArrayDeque<String>()
        writerOf[name]?.inputs?.forEach { stack.add(it.name) }
        while (stack.isNotEmpty()) {
            val port = stack.removeLast()
            if (!visited.add(port)) continue
            writerOf[port]?.inputs?.forEach { stack.add(it.name) }
        }
        visited.remove(name)
        return visited
    }

    /** All ports (transitively) computed from [name], not including [name] itself. */
    fun downstreamPorts(name: String): Set<String> {
        val visited = HashSet<String>()
        val stack = ArrayDeque<String>()
        readersOf[name]?.forEach { op -> op.outputs.forEach { stack.add(it.name) } }
        while (stack.isNotEmpty()) {
            val port = stack.removeLast()
            if (!visited.add(port)) continue
            readersOf[port]?.forEach { op -> op.outputs.forEach { stack.add(it.name) } }
        }
        visited.remove(name)
        return visited
    }

    /**
     * Data-flow edges between the given [anchors] (typically the ports the compositor shows as
     * tiles): an edge a -> b exists when some op path leads from a to b without passing through
     * any other anchor. This yields exactly the arrows a block diagram wants — e.g. residual
     * stream tiles chain layer to layer, with an extra hop through an attention-weights tile
     * where data really flows through it.
     */
    fun anchorEdges(anchors: Collection<String>): List<Pair<String, String>> {
        val anchorSet = anchors.toSet()
        val edges = LinkedHashSet<Pair<String, String>>()
        for (anchor in anchors) {
            val visited = HashSet<String>()
            val stack = ArrayDeque<String>()
            readersOf[anchor]?.forEach { op -> op.outputs.forEach { stack.add(it.name) } }
            while (stack.isNotEmpty()) {
                val port = stack.removeLast()
                if (!visited.add(port)) continue
                if (port in anchorSet) {
                    if (port != anchor) edges.add(anchor to port)
                    continue
                }
                readersOf[port]?.forEach { op -> op.outputs.forEach { stack.add(it.name) } }
            }
        }
        return edges.toList()
    }
}
