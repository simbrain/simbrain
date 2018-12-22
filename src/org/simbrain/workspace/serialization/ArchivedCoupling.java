package org.simbrain.workspace.serialization;

import org.simbrain.workspace.*;

/**
 * Class used to represent a coupling in the archive.
 *
 * @author Matt Watson
 */
class ArchivedCoupling {

    /**
     * The source attribute for the coupling.
     */
    private ArchivedAttribute producer;

    /**
     * The target attribute for the coupling.
     */
    private ArchivedAttribute consumer;

    /**
     * Creates a new instance.
     *
     * @param producer The producer attribute.
     * @param consumer The consumer attribute.
     */
    ArchivedCoupling(ArchivedAttribute producer, ArchivedAttribute consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }

    public ArchivedAttribute getProducer() {
        return producer;
    }

    public ArchivedAttribute getConsumer() {
        return consumer;
    }

    public Producer createProducer(Workspace workspace) {
        AttributeContainer object = getObjectFromWorkspace(workspace, producer);
        String method = producer.getMethodName();
        return CouplingUtils.getProducer(object, method);
    }

    public Consumer createConsumer(Workspace workspace) {
        AttributeContainer container = getObjectFromWorkspace(workspace, consumer);
        String method = consumer.getMethodName();
        return CouplingUtils.getConsumer(container, method);
    }

    private AttributeContainer getObjectFromWorkspace(Workspace workspace, ArchivedAttribute attribute) {
        WorkspaceComponent component = workspace.getComponent(attribute.getComponentId());
        AttributeContainer container = component.getObjectFromKey(attribute.getId());
        if (container == null) {
            throw new RuntimeException(String.format("Failed to retrieve object %s from serialized component %s.", attribute.getId(), attribute.getComponentId()));
        }
        return container;
    }

}
