package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.op.AddOp
import org.simbrain.network.tensor.op.LinearOp
import org.simbrain.network.tensor.op.OpPlan
import org.simbrain.network.tensor.op.TensorPort

class PlanGraphTest {

    private fun port(name: String, cols: Int = 4) = TensorPort(name, FloatTensor(1, cols))

    private fun weight(name: String) = TensorPort(name, FloatTensor(4, 4))

    /**
     * a = W1 x; b = W2 a; c = a + b; d = W3 c — a residual-style diamond where a feeds d both
     * through b and around it.
     */
    private fun diamond(): Pair<OpPlan, PlanGraph> {
        val x = port("x")
        val a = port("a")
        val b = port("b")
        val c = port("c")
        val d = port("d")
        val plan = OpPlan(listOf(
            LinearOp("l1", weight("W1"), x, a),
            LinearOp("l2", weight("W2"), a, b),
            AddOp("add", a, b, c),
            LinearOp("l3", weight("W3"), c, d),
        ))
        return plan to PlanGraph(plan)
    }

    @Test
    fun `upstream ports are the transitive inputs`() {
        val (_, graph) = diamond()
        assertEquals(setOf("a", "b", "x", "W1", "W2"), graph.upstreamPorts("c"))
        assertEquals(setOf("x", "W1"), graph.upstreamPorts("a"))
        assertTrue(graph.upstreamPorts("x").isEmpty())
    }

    @Test
    fun `downstream ports are the transitive outputs`() {
        val (_, graph) = diamond()
        assertEquals(setOf("b", "c", "d"), graph.downstreamPorts("a"))
        assertEquals(setOf("a", "b", "c", "d"), graph.downstreamPorts("x"))
        assertTrue(graph.downstreamPorts("d").isEmpty())
    }

    @Test
    fun `anchor edges stop at intermediate anchors but keep bypass paths`() {
        val (_, graph) = diamond()
        val edges = graph.anchorEdges(listOf("a", "b", "d")).toSet()
        assertEquals(setOf("a" to "b", "b" to "d", "a" to "d"), edges)
    }

    @Test
    fun `anchor edges through unanchored ports collapse to one hop`() {
        val (_, graph) = diamond()
        assertEquals(listOf("x" to "d"), graph.anchorEdges(listOf("x", "d")))
    }
}
