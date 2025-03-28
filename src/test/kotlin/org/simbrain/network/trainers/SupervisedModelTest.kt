package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import net.bytebuddy.implementation.bind.annotation.Super
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.math.SigmoidFunctionEnum
import smile.math.matrix.Matrix

class SupervisedModelTest {

    val net = Network()
    val inputArray = NeuronArray(10)
    val outputArray = NeuronArray(10)
    val wm = WeightMatrix(inputArray, outputArray)
    val sm = SupervisedModel(inputArray, outputArray)
    init {
        net.addNetworkModels(inputArray, outputArray, wm, sm)
    }

    @Test
    fun `deleting supervised model does not delete constituents`() = runBlocking {
        sm.delete()
        assertTrue(!net.allModels.contains(sm))
        assertTrue(net.allModels.contains(inputArray))
        assertTrue(net.allModels.contains(outputArray))
    }

    @Test
    fun `test supervised model serialization`() {
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        assertNotNull(fromXml.getModels(SupervisedModel::class.java).first())
    }

}