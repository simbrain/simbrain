package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.math.SigmoidFunctionEnum
import smile.math.matrix.Matrix

class SupervisedTrainerTest {

    val net = Network()
    val bp = BackpropNetwork(intArrayOf(10,8,10), null)

    @Test
    fun `test trainer state`() {
        val trainer = BackpropTrainer(net, bp)
        assertEquals(false, trainer.isRunning)
        runBlocking {
            trainer.startTraining()
            assertEquals(true, trainer.isRunning)
            trainer.stopTraining()
            assertEquals(false, trainer.isRunning)
        }
    }
}