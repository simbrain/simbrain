package org.simbrain.network.tensor.op

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.FloatTensor

class TapeTest {

    private class Fixture {
        val weight = TensorPort("weight", FloatTensor.of(2, 3, floatArrayOf(0.3f, -0.2f, 0.5f, 0.1f, 0.4f, -0.6f)))
        val x = TensorPort("x", FloatTensor.of(3, 1, floatArrayOf(1f, -2f, 0.5f)))
        val logits = TensorPort("logits", FloatTensor.vector(2))
        val probs = TensorPort("probs", FloatTensor.vector(2))
        val loss = TensorPort("loss", FloatTensor(1, 1))
        val plan = OpPlan(listOf(
            LinearOp("project", weight, x, logits),
            SoftmaxCrossEntropyOp("crossEntropy", logits, probs, loss),
        ))
    }

    @Test
    fun `micro-stepped backward matches atomic backward and runs VJPs in reverse order`() {
        val atomic = Fixture()
        val atomicTape = Tape()
        val atomicGrads = Gradients()
        atomic.plan.forward(atomicTape)
        atomicTape.backward(atomic.loss, atomicGrads)

        val stepped = Fixture()
        val tape = Tape()
        val grads = Gradients()
        stepped.plan.forward(tape)
        tape.beginBackward(stepped.loss, grads)
        assertTrue(tape.isBackwardInProgress)
        val order = mutableListOf<String>()
        while (tape.isBackwardInProgress) {
            order.add(tape.stepBackward(grads).name)
        }
        assertEquals(listOf("crossEntropy", "project"), order)
        assertArrayEquals(
            atomicGrads.of(atomic.weight.tensor).toFloatArray(),
            grads.of(stepped.weight.tensor).toFloatArray(),
            1e-7f
        )
        assertArrayEquals(
            atomicGrads.of(atomic.x.tensor).toFloatArray(),
            grads.of(stepped.x.tensor).toFloatArray(),
            1e-7f
        )
    }

    @Test
    fun `backward hooks fire once per stepped op and removed hooks stay silent`() {
        val fixture = Fixture()
        val tape = Tape()
        val grads = Gradients()
        val seen = mutableListOf<String>()
        val handle = tape.onBackwardStep { seen.add(it.name) }
        fixture.plan.forward(tape)
        tape.backward(fixture.loss, grads)
        assertEquals(listOf("crossEntropy", "project"), seen)

        handle.remove()
        tape.clear()
        grads.zeroAll()
        fixture.plan.forward(tape)
        tape.backward(fixture.loss, grads)
        assertEquals(2, seen.size)
    }

    @Test
    fun `stepBackward before beginBackward throws`() {
        val fixture = Fixture()
        val tape = Tape()
        fixture.plan.forward(tape)
        assertThrows(IllegalStateException::class.java) { tape.stepBackward(Gradients()) }
    }

    @Test
    fun `beginBackward while a pass is in progress throws`() {
        val fixture = Fixture()
        val tape = Tape()
        val grads = Gradients()
        fixture.plan.forward(tape)
        tape.beginBackward(fixture.loss, grads)
        assertThrows(IllegalStateException::class.java) { tape.beginBackward(fixture.loss, grads) }
    }

    @Test
    fun `clear abandons a mid-stepped backward pass`() {
        val fixture = Fixture()
        val tape = Tape()
        val grads = Gradients()
        fixture.plan.forward(tape)
        tape.beginBackward(fixture.loss, grads)
        tape.stepBackward(grads)
        tape.clear()
        assertFalse(tape.isBackwardInProgress)
        fixture.plan.forward(tape)
        tape.backward(fixture.loss, grads)
    }
}
