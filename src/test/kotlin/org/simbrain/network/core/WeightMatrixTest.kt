package org.simbrain.network.core

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import smile.math.matrix.Matrix
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.random.Random

class WeightMatrixTest {

    @Test
    fun `psr matrix keeps the weight shape when a transposed matrix is assigned`() {
        val wm = WeightMatrix(NeuronArray(3), NeuronArray(2))
        wm.psrMatrix = Matrix(3, 2)
        assertEquals(2, wm.psrMatrix.nrow())
        assertEquals(3, wm.psrMatrix.ncol())
    }

    @Test
    fun `network with a transposed saved psr matrix loads and updates`() {
        val net = Network()
        val source = NeuronArray(3)
        val target = NeuronArray(2)
        net.addNetworkModelsAsync(source, target, WeightMatrix(source, target))
        // Older builds wrote the psr matrix transposed after the weight matrix dialog was committed
        val xml = getNetworkXStream().toXML(net).replace(
            Regex("(<psrMatrix[^>]*>\\s*<rows>)2(</rows>\\s*<cols>)3(</cols>)"), "$13$22$3"
        )
        val loaded = getNetworkXStream().fromXML(xml) as Network
        val wm = loaded.getModels<WeightMatrix>().first()
        assertEquals(2, wm.psrMatrix.nrow())
        assertEquals(3, wm.psrMatrix.ncol())
        runBlocking { loaded.update() }
    }

    @Test
    fun `reading the psr matrix during updates leaves each update's inputs intact`() {
        val net = Network()
        val size = 200
        val source = NeuronArray(size).apply { isClamped = true }
        val target = NeuronArray(size)
        val wm = WeightMatrix(source, target)
        net.addNetworkModelsAsync(source, target, wm)
        // Small weights keep every input inside the linear rule's bounds
        val random = Random(1)
        for (i in 0 until size) for (j in 0 until size) wm.weights[i, j] = random.nextDouble(0.0, 0.01)

        // Reads the matrix the way the property editor, copying and saving do, from off the update thread
        val stop = AtomicBoolean(false)
        val reader = thread(isDaemon = true) {
            while (!stop.get()) wm.psrMatrix
        }
        try {
            repeat(2000) { step ->
                // New source activations each step, so a matrix left half filled from the step before shows up
                val x = DoubleArray(size) { ((it + step) % 7 - 3).toDouble() }
                source.setActivations(x)
                runBlocking { net.update() }
                val expected = DoubleArray(size) { i -> (0 until size).sumOf { j -> wm.weights[i, j] * x[j] } }
                assertArrayEquals(expected, target.activations.col(0), 1e-9, "step $step")
            }
        } finally {
            stop.set(true)
            reader.join()
        }
    }
}
