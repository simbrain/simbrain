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
    private val opIndex = HashMap<TensorOp, Int>()

    init {
        for ((i, op) in plan.ops.withIndex()) {
            opIndex[op] = i
            for (port in op.outputs) writerOf[port.name] = op
            for (port in op.inputs) readersOf.getOrPut(port.name) { mutableListOf() }.add(op)
        }
    }

    /** Position of the op that writes [name] in the plan's schedule, or null for pure inputs. */
    fun writerIndex(name: String): Int? = writerOf[name]?.let { opIndex.getValue(it) }

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
     * where data really flows through it. Each edge carries the ops crossed on the way (from at
     * least one such path), so renderers can decorate edges with operation glyphs.
     */
    fun anchorEdges(anchors: Collection<String>): List<EdgePath> {
        val anchorSet = anchors.toSet()
        val edges = LinkedHashMap<Pair<String, String>, LinkedHashSet<TensorOp>>()
        for (anchor in anchors) {
            val visited = HashSet<String>()
            val stack = ArrayDeque<Pair<String, List<TensorOp>>>()
            readersOf[anchor]?.forEach { op -> op.outputs.forEach { stack.add(it.name to listOf(op)) } }
            while (stack.isNotEmpty()) {
                val (port, opsSoFar) = stack.removeLast()
                if (port in anchorSet) {
                    if (port != anchor) edges.getOrPut(anchor to port) { LinkedHashSet() }.addAll(opsSoFar)
                    continue
                }
                if (!visited.add(port)) continue
                readersOf[port]?.forEach { op -> op.outputs.forEach { stack.add(it.name to (opsSoFar + op)) } }
            }
        }
        return edges.map { (endpoints, ops) -> EdgePath(endpoints.first, endpoints.second, ops.toList()) }
    }

    /** The op that writes [name], or null for pure inputs (parameters, clamped ports). */
    fun writer(name: String): TensorOp? = writerOf[name]

    /** The ops that read [name] directly. */
    fun readers(name: String): List<TensorOp> = readersOf[name] ?: emptyList()

    /** Position of [op] in the plan's schedule. */
    fun scheduleIndex(op: TensorOp): Int? = opIndex[op]

    /**
     * The ops on paths between [anchors] that join two or more displayed data streams — inputs
     * that are anchors themselves or are computed by other ops (pure parameters don't count).
     * These are the diagram's junction points: the residual rejoins, q x k, attention x values,
     * embedding + positions.
     */
    fun junctionOps(anchors: Collection<String>): Set<TensorOp> {
        val anchorSet = anchors.toSet()
        val onPaths = anchorEdges(anchors).flatMapTo(HashSet()) { it.ops }
        return onPaths.filterTo(LinkedHashSet()) { op ->
            op.inputs.count { it.name in anchorSet || it.name in writerOf } >= 2
        }
    }

    /**
     * Like [anchorEdges], but walks also stop at [junctions]: the result connects anchors AND
     * junction ops, so a multi-input op renders as a vertex its input streams' arrows converge
     * into, instead of a glyph strung on one arbitrary edge.
     */
    fun displayEdges(anchors: Collection<String>, junctions: Set<TensorOp>): List<DisplaySegment> {
        val anchorSet = anchors.toSet()
        val edges = LinkedHashMap<Pair<Any, Any>, LinkedHashSet<TensorOp>>()

        fun walk(sourceKey: Any, startPorts: List<String>) {
            val visited = HashSet<String>()
            val stack = ArrayDeque<Pair<String, List<TensorOp>>>()

            fun expand(port: String, opsSoFar: List<TensorOp>) {
                for (op in readersOf[port].orEmpty()) {
                    if (op in junctions) {
                        edges.getOrPut(sourceKey to op) { LinkedHashSet() }.addAll(opsSoFar)
                        continue
                    }
                    for (out in op.outputs) {
                        if (out.name in anchorSet) {
                            edges.getOrPut(sourceKey to out.name) { LinkedHashSet() }.addAll(opsSoFar + op)
                        } else {
                            stack.add(out.name to (opsSoFar + op))
                        }
                    }
                }
            }

            for (port in startPorts) expand(port, emptyList())
            while (stack.isNotEmpty()) {
                val (port, opsSoFar) = stack.removeLast()
                if (!visited.add(port)) continue
                expand(port, opsSoFar)
            }
        }

        for (anchor in anchors) walk(anchor, listOf(anchor))
        for (junction in junctions) {
            val (anchorOuts, innerOuts) = junction.outputs.partition { it.name in anchorSet }
            // A junction writing an anchor port directly is an edge on its own.
            for (out in anchorOuts) edges.getOrPut(junction as Any to out.name) { LinkedHashSet() }
            walk(junction, innerOuts.map { it.name })
        }
        return edges.map { (endpoints, ops) -> DisplaySegment(endpoints.first, endpoints.second, ops.toList()) }
    }
}

/** One derived diagram edge: anchor [from] to anchor [to], with the [ops] crossed between them. */
class EdgePath(val from: String, val to: String, val ops: List<TensorOp>)

/** A display-graph edge; each end is a port name (an anchor tile) or a junction [TensorOp]. */
class DisplaySegment(val from: Any, val to: Any, val ops: List<TensorOp>)
