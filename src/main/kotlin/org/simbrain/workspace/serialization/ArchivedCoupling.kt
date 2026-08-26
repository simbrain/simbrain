/**
 * Archive form of a [org.simbrain.workspace.couplings.Coupling]: the producer and consumer as
 * [ArchivedAttribute]s plus the coupling's transform chain, which is serialized as the operation objects
 * themselves. The transforms field is nullable because archives written before transforms existed have
 * no such element; restore treats null as an empty chain.
 */
package org.simbrain.workspace.serialization

import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumer
import org.simbrain.workspace.Producer
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.couplings.CouplingOperation

class ArchivedCoupling(
    val producer: ArchivedAttribute,
    val consumer: ArchivedAttribute,
    val transforms: List<CouplingOperation<*, *>>? = null
) {

    fun createProducer(workspace: Workspace): Producer = with(workspace.couplingManager) {
        getObjectFromWorkspace(workspace, producer).getProducer(producer.methodName)
    }

    fun createConsumer(workspace: Workspace): Consumer = with(workspace.couplingManager) {
        getObjectFromWorkspace(workspace, consumer).getConsumer(consumer.methodName)
    }

    /**
     * Find the attribute container corresponding to an archived attribute object.
     */
    private fun getObjectFromWorkspace(workspace: Workspace, attribute: ArchivedAttribute): AttributeContainer {
        val component = workspace.getComponent(attribute.componentId)
            ?: throw RuntimeException("No component ${attribute.componentId} found for archived attribute ${attribute.attributeId}.")
        return component.attributeContainers.find { it.id == attribute.attributeId }
            ?: throw RuntimeException(
                "Failed to retrieve object ${attribute.attributeId} from serialized component ${attribute.componentId}."
            )
    }
}
