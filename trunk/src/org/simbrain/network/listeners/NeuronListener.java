
package org.simbrain.network.listeners;

import org.simbrain.network.interfaces.Neuron;

/**
 * Listener interface for receiving events relating to neurons. Classes
 * interested in responding to such events are registered with a RootNetwork,
 * which broadcasts those events to registered observer classes.
 */
public interface NeuronListener {

    /**
     * Notify this listener of a Neuron changed event.
     *
     * @param networkEvent holds reference to changed neuron
     */
    void neuronChanged(NetworkEvent<Neuron> networkEvent);

    /**
     * Notify this listener of a Neuron type changed event.
     *
     * @param networkEvent holds reference to old and new Neuron
     */
    void neuronTypeChanged(NetworkEvent<Neuron> networkEvent);

    /**
     * Notify this listener of a Neuron added event.
     *
     * @param networkEvent reference to new neuron
     */
    void neuronAdded(NetworkEvent<Neuron> networkEvent);

    /**
     * Notify this listener of a Neuron moved event.
     *
     * @param networkEvent reference to neuron
     */
    void neuronMoved(NetworkEvent<Neuron> networkEvent);

    /**
     * Notify this listener of a Neuron removed event.
     *
     * @param networkEvent reference to Neuron
     */
    void neuronRemoved(NetworkEvent<Neuron> networkEvent);
}