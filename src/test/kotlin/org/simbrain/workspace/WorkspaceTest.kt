package org.simbrain.workspace

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.workspace.couplings.getConsumer
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.serialization.WorkspaceSerializer
import org.simbrain.world.odorworld.OdorWorldComponent
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class WorkspaceTest {
    var workspace: Workspace? = null

    val n1 = Neuron()
    var n2 = Neuron()

    @BeforeEach
    fun setUpTestWorkspace() {
        workspace = Workspace()
        val net1 = Network()
        val nc1 = NetworkComponent("Net1", net1)
        val net2 = Network()
        val nc2 = NetworkComponent("Net2", net2)
        workspace!!.addWorkspaceComponent(nc1)
        workspace!!.addWorkspaceComponent(nc2)
        workspace!!.addWorkspaceComponent(OdorWorldComponent("odorworld"))
        workspace!!.addWorkspaceComponent(ProjectionComponent("projection"))

        // Add a neuron to network 1
        net1.addNetworkModel(n1)

        // Add a neuron to network 2
        net2.addNetworkModel(n2)

        // Couple them
        workspace!!.couplingManager.createCoupling(
            n1.getProducer(Neuron::activation),
            n2.getConsumer(Neuron::addInputValue)
        )
    }

    @Test
    fun testComponents() {
        Assertions.assertEquals(4, workspace!!.componentList.size)
    }

    @Test
    fun testCouplings() {
        Assertions.assertEquals(1, workspace!!.couplingManager.couplings.size)
        n1.activation = .8
        workspace!!.simpleIterate()
        Assertions.assertEquals(.8, n2.activation, .0001)
    }

    @Test
    @Throws(IOException::class)
    fun testSerialization() {
        val serializer = WorkspaceSerializer(workspace!!)

        // "Save" to output stream
        val bas = ByteArrayOutputStream()
        serializer.serialize(bas, true)
        bas.close()

        // Clear workspace
        workspace!!.clearWorkspace()

        // Create an input stream from the output stream
        val bis = ByteArrayInputStream(bas.toByteArray())

        // "Open" from the input stream
        runBlocking {
            serializer.deserialize(bis)
        }
        bis.close()

        // Check everything is as expected in the deserialized net
        Assertions.assertEquals(4, workspace!!.componentList.size)
        Assertions.assertEquals(1, workspace!!.couplingManager.couplings.size)

        // Can't reuse n1 and n2 because it's been deserialized
        val newN1 = (workspace!!.getComponent("Net1") as NetworkComponent).network.allModels[0] as Neuron
        val newN2 = (workspace!!.getComponent("Net2") as NetworkComponent).network.allModels[0] as Neuron
        newN1.activation = .8
        workspace!!.simpleIterate()
        Assertions.assertEquals(.8, newN2.activation, .0001)
    }

    @Test
    @Throws(IOException::class)
    fun testZipMethods() {
        val byteArray = workspace!!.zipDataHeadless
        runBlocking { workspace!!.openFromZipData(byteArray) }

        // Check everything is as expected in the deserialized net
        Assertions.assertEquals(4, workspace!!.componentList.size)
        Assertions.assertEquals(1, workspace!!.couplingManager.couplings.size)

        // Can't reuse n1 and n2 because it's been deserialized
        val newN1 = (workspace!!.getComponent("Net1") as NetworkComponent).network.allModels[0] as Neuron
        val newN2 = (workspace!!.getComponent("Net2") as NetworkComponent).network.allModels[0] as Neuron
        newN1.activation = .8
        workspace!!.simpleIterate()
        Assertions.assertEquals(.8, newN2.activation, .0001)
    }
}