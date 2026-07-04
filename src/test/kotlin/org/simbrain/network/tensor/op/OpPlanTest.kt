package org.simbrain.network.tensor.op

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.FloatTensor

class OpPlanTest {

    private class ScaleOp(name: String, val src: TensorPort, val dst: TensorPort, val factor: Float) : TensorOp(name) {
        override val inputs = listOf(src)
        override val outputs = listOf(dst)
        override fun forward() {
            for (i in 0 until dst.tensor.size) {
                dst.tensor.data.put(i, src.tensor.data.get(i) * factor)
            }
        }
    }

    private fun vec(vararg values: Float) = FloatTensor.of(1, values.size, values)

    @Test
    fun `forward runs ops in order and hooks fire with produced values`() {
        val x = TensorPort("x", vec(1f, 2f))
        val doubled = TensorPort("doubled", FloatTensor(1, 2))
        val tripled = TensorPort("tripled", FloatTensor(1, 2))
        val plan = OpPlan(listOf(
            ScaleOp("double", x, doubled, 2f),
            ScaleOp("triple", doubled, tripled, 3f),
        ))

        val seen = mutableListOf<Float>()
        plan.onPort("doubled") { seen.add(it.tensor[0, 0]) }
        plan.onPort("tripled") { seen.add(it.tensor[0, 0]) }
        plan.forward()

        assertEquals(listOf(2f, 6f), seen)
        assertEquals(6f, plan.port("tripled").tensor[0, 0])
        assertEquals(12f, plan.port("tripled").tensor[0, 1])
    }

    @Test
    fun `removed hook no longer fires`() {
        val x = TensorPort("x", vec(1f))
        val y = TensorPort("y", FloatTensor(1, 1))
        val plan = OpPlan(listOf(ScaleOp("double", x, y, 2f)))
        var calls = 0
        val handle = plan.onPort("y") { calls++ }
        plan.forward()
        handle.remove()
        plan.forward()
        assertEquals(1, calls)
    }

    @Test
    fun `micro stepping runs one op at a time and wraps at the pass boundary`() {
        val x = TensorPort("x", vec(1f))
        val a = TensorPort("a", FloatTensor(1, 1))
        val b = TensorPort("b", FloatTensor(1, 1))
        val plan = OpPlan(listOf(ScaleOp("first", x, a, 2f), ScaleOp("second", a, b, 5f)))

        assertEquals("first", plan.stepOp().name)
        assertEquals(1, plan.cursor)
        assertEquals("second", plan.stepOp().name)
        assertEquals(0, plan.cursor)
        assertEquals(10f, b.tensor[0, 0])
    }

    @Test
    fun `forward mid-step throws`() {
        val x = TensorPort("x", vec(1f))
        val a = TensorPort("a", FloatTensor(1, 1))
        val b = TensorPort("b", FloatTensor(1, 1))
        val plan = OpPlan(listOf(ScaleOp("first", x, a, 2f), ScaleOp("second", a, b, 5f)))
        plan.stepOp()
        assertThrows(IllegalStateException::class.java) { plan.forward() }
    }

    @Test
    fun `plan bumps output versions for dirty tracking`() {
        val x = TensorPort("x", vec(1f))
        val y = TensorPort("y", FloatTensor(1, 1))
        val plan = OpPlan(listOf(ScaleOp("double", x, y, 2f)))
        val before = y.tensor.version
        plan.forward()
        assertTrue(y.tensor.version > before)
    }

    @Test
    fun `two writers for one port are rejected`() {
        val x = TensorPort("x", vec(1f))
        val y = TensorPort("y", FloatTensor(1, 1))
        assertThrows(IllegalArgumentException::class.java) {
            OpPlan(listOf(ScaleOp("first", x, y, 2f), ScaleOp("second", x, y, 3f)))
        }
    }

    @Test
    fun `duplicate port names on different tensors are rejected`() {
        val x1 = TensorPort("x", vec(1f))
        val x2 = TensorPort("x", vec(1f))
        val a = TensorPort("a", FloatTensor(1, 1))
        val b = TensorPort("b", FloatTensor(1, 1))
        assertThrows(IllegalArgumentException::class.java) {
            OpPlan(listOf(ScaleOp("first", x1, a, 2f), ScaleOp("second", x2, b, 3f)))
        }
    }

    @Test
    fun `shared port object between producer and consumer registers once`() {
        val x = TensorPort("x", vec(1f))
        val mid = TensorPort("mid", FloatTensor(1, 1))
        val out = TensorPort("out", FloatTensor(1, 1))
        val plan = OpPlan(listOf(ScaleOp("produce", x, mid, 2f), ScaleOp("consume", mid, out, 3f)))
        assertSame(mid, plan.port("mid"))
    }

    @Test
    fun `recording an op without backward throws`() {
        val x = TensorPort("x", vec(1f))
        val y = TensorPort("y", FloatTensor(1, 1))
        val plan = OpPlan(listOf(ScaleOp("double", x, y, 2f)))
        assertTrue(!plan.trainable)
        assertThrows(IllegalArgumentException::class.java) { plan.forward(Tape()) }
    }

    @Test
    fun `recording an in-place op throws`() {
        val x = TensorPort("x", vec(1f, 2f))
        val y = TensorPort("y", FloatTensor(1, 2))
        val inPlace = object : TensorOp("accumulate") {
            override val inputs = listOf(x, y)
            override val outputs = listOf(y)
            override val hasBackward get() = true
            override fun forward() {}
            override fun backward(grads: Gradients) {}
        }
        val plan = OpPlan(listOf(inPlace))
        plan.forward()
        assertThrows(IllegalArgumentException::class.java) { plan.forward(Tape()) }
    }

    @Test
    fun `hook on a writerless input port throws`() {
        val x = TensorPort("x", vec(1f))
        val y = TensorPort("y", FloatTensor(1, 1))
        val plan = OpPlan(listOf(ScaleOp("double", x, y, 2f)))
        assertThrows(IllegalStateException::class.java) { plan.onPort("x") {} }
    }

    @Test
    fun `backward on a saved tensor mutated after record throws`() {
        val x = TensorPort("x", vec(1f, -2f, 3f))
        val gate = TensorPort("gate", vec(0.5f, 1f, -1f))
        val out = TensorPort("out", FloatTensor(1, 3))
        val plan = OpPlan(listOf(SiluGateOp("gate", x, gate, out)))
        val tape = Tape()
        plan.forward(tape)
        x.tensor[0, 0] = 99f
        assertThrows(IllegalStateException::class.java) {
            tape.backward(TensorPort("fake", FloatTensor(1, 1)), Gradients())
        }
    }
}
