package org.simbrain.workspace.serialization;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

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
        Object object = getObjectFromWorkspace(workspace, producer);
        String method = producer.getMethodName();
        return workspace.getCouplingManager().getProducer(object, method);
    }

    public Consumer createConsumer(Workspace workspace) {
        Object object = getObjectFromWorkspace(workspace, consumer);
        String method = consumer.getMethodName();
        return workspace.getCouplingManager().getConsumer(object, method);
    }

    private Object getObjectFromWorkspace(Workspace workspace, ArchivedAttribute attribute) {
        WorkspaceComponent component = workspace.getComponent(attribute.getComponentId());
        Object object = component.getObjectFromKey(attribute.getId());
        if (object == null) {
            throw new RuntimeException(String.format("Failed to retrieve object %s from serialized component %s.", attribute.getId(), attribute.getComponentId()));
        }
        return object;
    }

}
