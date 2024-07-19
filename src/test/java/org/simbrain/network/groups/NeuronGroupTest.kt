package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.neurongroups.SoftmaxGroup
import java.util.List

class NeuronGroupTest {
    var net: Network = Network()
    var ng: NeuronGroup = NeuronGroup(2)

    init {
        ng.label = "test"
        net.addNetworkModel(ng)
    }


    @Test
    fun testCopy() {
        val ng2 = ng.copy()
        net.addNetworkModel(ng2)
        Assertions.assertEquals(2, ng2.neuronList.size)
    }

    @Test
    fun propagateLooseActivations() {
        ng.getNeuron(0).activation = 1.0
        ng.getNeuron(1).activation = -1.0
        val ng2 = NeuronGroup(2)
        val wm = WeightMatrix(ng, ng2)
        net.addNetworkModels(List.of(ng2, wm))
        net.update()
        Assertions.assertArrayEquals(doubleArrayOf(1.0, -1.0), ng2.activationArray)
    }

    @Test
    fun propagateGroupActivation() {
        ng.activationArray = doubleArrayOf(1.0, -1.0)
        val ng2 = NeuronGroup(2)
        val wm = WeightMatrix(ng, ng2)
        net.addNetworkModels(List.of(ng2, wm))
        net.update()
        Assertions.assertArrayEquals(doubleArrayOf(1.0, -1.0), ng2.activationArray)
    }


    @Test
    fun getThenSetActivations() {
            ng.activations // validates the cache. be sure propagation still works
            ng.getNeuron(0).activation = 1.0
            ng.getNeuron(1).activation = -1.0
            val ng2 = NeuronGroup(2)
            val wm = WeightMatrix(ng, ng2)
            net.addNetworkModels(List.of(ng2, wm))
            net.update()
            Assertions.assertArrayEquals(doubleArrayOf(1.0, -1.0), ng2.activationArray)
        }

    @Test
    fun testSoftmax() {
        with(net) {
            ng.randomize()
            val ng2 = SoftmaxGroup(5)
            val wm = WeightMatrix(ng, ng2)
            net.addNetworkModels(ng2, wm)
            net.update()
            // System.out.println(Arrays.toString(ng2.getActivations()));
            Assertions.assertEquals(1.0, ng2.activations.sum(), .01)
        }
    }
}