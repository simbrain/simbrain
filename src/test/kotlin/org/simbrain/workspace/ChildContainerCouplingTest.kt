/**
 * Tests that couplings whose endpoints are child attribute containers (containers reached through a
 * model's `childrenContainers` rather than listed directly by a component) survive workspace
 * serialization and reconnect to the matching child after load.
 */
package org.simbrain.workspace

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ChildContainerCouplingTest {

    @Test
    fun `tensor channel coupling survives workspace serialization`() {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("Net")
        workspace.addWorkspaceComponent(networkComponent)
        val source = TensorLayer(TensorShape(2, 2, 2))
        val target = TensorLayer(TensorShape(2, 2, 2))
        networkComponent.network.addNetworkModelAsync(source)
        networkComponent.network.addNetworkModelAsync(target)
        with(workspace.couplingManager) {
            source.channelContainers[1].getProducer("getValues") couple
                target.channelContainers[0].getConsumer("setValues")
        }

        val serializer = WorkspaceSerializer(workspace)
        val bas = ByteArrayOutputStream()
        serializer.serialize(bas, true)
        workspace.clearWorkspace()
        runBlocking { serializer.deserialize(ByteArrayInputStream(bas.toByteArray())) }

        val restored = workspace.getComponent("Net") as NetworkComponent
        val (restoredSource, restoredTarget) = restored.network.getModels<TensorLayer>().toList()
        val coupling = workspace.couplingManager.couplings.single()
        assertEquals(restoredSource.channelContainers[1], coupling.producer.baseObject)
        assertEquals(restoredTarget.channelContainers[0], coupling.consumer.baseObject)

        restoredSource.setChannel(1, doubleArrayOf(1.0, 2.0, 3.0, 4.0))
        runBlocking { workspace.couplingManager.updateCouplings() }
        val delivered = DoubleArray(4)
        restoredTarget.getChannel(0, delivered)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0), delivered)
    }
}
