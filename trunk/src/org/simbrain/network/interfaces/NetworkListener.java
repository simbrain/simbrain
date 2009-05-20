
package org.simbrain.network.interfaces;

/**
 * Model listener.
 */
public interface NetworkListener {
    /**
     * Notify this listener of a NetworkChanged event.
     *
     */
    void networkChanged();

    /**
     * Notify this listener of a NeuronChanged event.
     *
     * @param e event
     */
    void neuronChanged(NetworkEvent<Neuron> e);

    /**
     * Notify this listener of a Neuron added event.
     *
     * @param e event
     */
    void neuronAdded(NetworkEvent<Neuron> e);

    /**
     * Notify this listener of a Neuron moved event.
     *
     * @param e event
     */
    void neuronMoved(NetworkEvent<Neuron> e);

    /**
     * Notify this listener of a Neuron removed event.
     *
     * @param e event
     */
    void neuronRemoved(NetworkEvent<Neuron> e);

    /**
     * Notify this listener of a Neuron removed event.
     *
     * @param e event
     */
    void synapseRemoved(NetworkEvent<Synapse> e);

    /**
     * Notify this listener of a Neuron removed event.
     *
     * @param e event
     */
    void synapseAdded(NetworkEvent<Synapse> e);

    /**
     * Notify this listener of a Neuron removed event.
     *
     * @param e event
     */
    void synapseChanged(NetworkEvent<Synapse> e);

    /**
     * Notify this listener of a subnetwork added event.
     *
     * @param e event
     */
    void subnetAdded(NetworkEvent<Network> e);

    /**
     * Notify this listener of a subnetwork removed event.
     *
     * @param e event
     */
    void subnetRemoved(NetworkEvent<Network> e);

    /**
     * A group has been added.
     *
     * @param event the event, which contains a reference to the new group.
     */
    void groupAdded(NetworkEvent<Group> event);

    /**
     * A group has been changed.
     *
     * @param event the event, which contains a reference to the old and changed group.
     */
    void groupChanged(NetworkEvent<Group> event);

    /**
     * A group has been removed.
     *
     * @param event the event, which contains a reference to the group to be removed.
     */
    void groupRemoved(NetworkEvent<Group> event);

    /**
     * Notify listeners of a clamp changed event, when the status
     * of the "clamp fields" changes.
     * 
     * TODO: Perhaps later this can be generaliezd to "parameter change" events, if there are more such parameters.
     *
     */
    void clampMenuChanged();

    void clampBarChanged();
}