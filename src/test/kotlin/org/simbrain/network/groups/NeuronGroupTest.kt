package org.simbrain.network.groups

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.neurongroups.*
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.List

class NeuronGroupTest {

    val workspace = Workspace()
    val networkComponent = NetworkComponent("Test")
    var net = networkComponent.network
    var ng: NeuronGroup = NeuronGroup(2)

    init {
        ng.label = "test"
        net.addNetworkModel(ng)
        workspace.addWorkspaceComponent(networkComponent)
    }

    @Test
    fun testCopy() {
        val ng2 = ng.copy()
        net.addNetworkModel(ng2)
        assertEquals(2, ng2.neuronList.size)
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
            assertEquals(1.0, ng2.activations.sum(), .01)
        }
    }

    @Test
    fun `test serialization with couplings`() {

        with(workspace.couplingManager) {
            ng.getNeuron(0) couple ng.getNeuron(1)
        }

        val serializer = WorkspaceSerializer(workspace)
        val bas = ByteArrayOutputStream()
        serializer.serialize(bas, true)
        bas.close()
        workspace.clearWorkspace()

        // Reopen
        val bis = ByteArrayInputStream(bas.toByteArray())
        runBlocking {
            serializer.deserialize(bis)
        }
        bis.close()
        assertEquals(1, workspace.couplingManager.couplings.size)

    }

}