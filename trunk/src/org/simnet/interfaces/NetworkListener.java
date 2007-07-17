
package org.simnet.interfaces;

import java.util.EventListener;

/**
 * Model listener.
 */
public interface NetworkListener extends EventListener {

    /**
     * Notify this listener of a NetworkChanged event.
     *
     */
    void networkChanged();

    /**
     * Notify this listener of a CouplingChanged event.
     * Pass in a reference to the neuron on which the coupling changed.
     * @param e Network event
     */
    void couplingChanged(NetworkEvent e);

    /**
     * Notify this listener of a NeuronChanged event.
     *
     * @param e event
     */
    void neuronChanged(NetworkEvent e);

    /**
     * Notify this listener of a Neuron added event.
     *
     * @param e event
     */
    void neuronAdded(NetworkEvent e);

    /**
     * Notify this listener of a Neuron moved event.
     *
     * @param e event
     */
    void neuronMoved(NetworkEvent e);

    /**
     * Notify this listener of a Neuron removed event.
     *
     * @param e event
     */
    void neuronRemoved(NetworkEvent e);

    /**
     * Notify this listener of a Neuron removed event.
     *
     * @param e event
     */
    void synapseRemoved(NetworkEvent e);

    /**
     * Notify this listener of a Neuron removed event.
     *
     * @param e event
     */
    void synapseAdded(NetworkEvent e);

    /**
     * Notify this listener of a Neuron removed event.
     *
     * @param e event
     */
    void synapseChanged(NetworkEvent e);

    /**
     * Notify this listener of a subnetwork added event.
     *
     * @param e event
     */
    void subnetAdded(NetworkEvent e);

    /**
     * Notify this listener of a subnetwork removed event.
     *
     * @param e event
     */
    void subnetRemoved(NetworkEvent e);

    /**
     * Notify listeners of a clamp changed event, when the statu
     * of the "clamp fields" changes.
     * 
     * TODO: Perhaps later this can be generaliezd to "parameter change" events, if there are more such parameters.
     *
     */
    void clampMenuChanged();

    void clampBarChanged();

    void groupAdded(NetworkEvent event);
    
    void groupChanged(NetworkEvent event);
    
    void groupRemoved(NetworkEvent event);
}