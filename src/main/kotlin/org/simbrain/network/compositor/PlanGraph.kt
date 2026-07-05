package org.simbrain.network.compositor

import org.simbrain.network.tensor.op.OpPlan
import org.simbrain.network.tensor.op.TensorOp

/**
 * Port-level adjacency over an [OpPlan]: which op writes each port and which ops read it.
 * This is what makes compositor layout and trace derivable from the plan instead of
 * hand-coded — upstream/downstream reachability and tile-to-tile edges all fall out of
 * the op list.
 *
 * An optional [aliasFn] projects port and op names into a display namespace before adjacency is
 * built. Ops whose names alias to the same string merge into one graph node, represented by the
 * first such op (its class identity drives icons and strand detection); the node's inputs and
 * outputs are the union over the merged ops. This is how a stacked structure view projects two
 * representative layers onto one canonical block — e.g. both layers' `mixer_residual` adds
 * become a single junction whose input streams are the residual bypass and both mixer limbs.
 * Every name-typed argument and result of this class lives in the aliased namespace.
 */
class PlanGraph(val plan: OpPlan, private val aliasFn: (String) -> String = { it }) {

    private val writerOf = HashMap<String, TensorOp>()
    private val readersOf = HashMap<String, MutableList<TensorOp>>()
    private val opIndex = HashMap<TensorOp, Int>()

    // Inputs are keyed by (name, occurrence within the op): an op reading the same stream twice
    // (a gate joining two slices of one projection) keeps both entries and counts as a junction,
    // while merged ops that each read the shared stream once collapse to a single entry.
    private val nodeInputs = HashMap<TensorOp, LinkedHashSet<Pair<String, Int>>>()
    private val nodeOutputs = HashMap<TensorOp, LinkedHashSet<String>>()

    init {
        val representatives = HashMap<String, TensorOp>()
        for ((i, op) in plan.ops.withIndex()) {
            val node = representatives.getOrPut(aliasFn(op.name)) { op }
            opIndex.putIfAbsent(node, i)
            val outputs = nodeOutputs.getOrPut(node) { LinkedHashSet() }
            for (port in op.outputs) {
                val name = aliasFn(port.name)
                outputs.add(name)
                writerOf[name] = node
            }
            val inputs = nodeInputs.getOrPut(node) { LinkedHashSet() }
            val occurrences = HashMap<String, Int>()
            for (port in op.inputs) {
                val name = aliasFn(port.name)
                inputs.add(name to occurrences.merge(name, 1, Int::plus)!!)
                val readers = readersOf.getOrPut(name) { mutableListOf() }
                if (node !in readers) readers.add(node)
            }
        }
    }

    /** Projects a raw plan name into this graph's display namespace. */
    fun alias(name: String) = aliasFn(name)

    private fun outputsOf(op: TensorOp): Set<String> = nodeOutputs[op] ?: emptySet()

    /** Position of the op that writes [name] in the plan's schedule, or null for pure inputs. */
    fun writerIndex(name: String): Int? = writerOf[name]?.let { opIndex.getValue(it) }

    /** All ports whose values (transitively) feed [name], not including [name] itself. */
    fun upstreamPorts(name: String): Set<String> {
        val visited = HashSet<String>()
        val stack = ArrayDeque<String>()
        writerOf[name]?.let { nodeInputs[it] }?.forEach { stack.add(it.first) }
        while (stack.isNotEmpty()) {
            val port = stack.removeLast()
            if (!visited.add(port)) continue
            writerOf[port]?.let { nodeInputs[it] }?.forEach { stack.add(it.first) }
        }
        visited.remove(name)
        return visited
    }

    /** All ports (transitively) computed from [name], not including [name] itself. */
    fun downstreamPorts(name: String): Set<String> {
        val visited = HashSet<String>()
        val stack = ArrayDeque<String>()
        readersOf[name]?.forEach { op -> outputsOf(op).forEach { stack.add(it) } }
        while (stack.isNotEmpty()) {
            val port = stack.removeLast()
            if (!visited.add(port)) continue
            readersOf[port]?.forEach { op -> outputsOf(op).forEach { stack.add(it) } }
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
            readersOf[anchor]?.forEach { op -> outputsOf(op).forEach { stack.add(it to listOf(op)) } }
            while (stack.isNotEmpty()) {
                val (port, opsSoFar) = stack.removeLast()
                if (port in anchorSet) {
                    if (port != anchor) edges.getOrPut(anchor to port) { LinkedHashSet() }.addAll(opsSoFar)
                    continue
                }
                if (!visited.add(port)) continue
                readersOf[port]?.forEach { op -> outputsOf(op).forEach { stack.add(it to (opsSoFar + op)) } }
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
            nodeInputs.getValue(op).count { (name, _) -> name in anchorSet || name in writerOf } >= 2
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
                    for (out in outputsOf(op)) {
                        if (out in anchorSet) {
                            edges.getOrPut(sourceKey to out) { LinkedHashSet() }.addAll(opsSoFar + op)
                        } else {
                            stack.add(out to (opsSoFar + op))
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
            val (anchorOuts, innerOuts) = outputsOf(junction).partition { it in anchorSet }
            // A junction writing an anchor port directly is an edge on its own.
            for (out in anchorOuts) edges.getOrPut(junction as Any to out) { LinkedHashSet() }
            walk(junction, innerOuts)
        }
        return edges.map { (endpoints, ops) -> DisplaySegment(endpoints.first, endpoints.second, ops.toList()) }
    }
}

/** One derived diagram edge: anchor [from] to anchor [to], with the [ops] crossed between them. */
class EdgePath(val from: String, val to: String, val ops: List<TensorOp>)

/** A display-graph edge; each end is a port name (an anchor tile) or a junction [TensorOp]. */
class DisplaySegment(val from: Any, val to: Any, val ops: List<TensorOp>)
