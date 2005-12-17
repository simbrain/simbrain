
package org.simnet.interfaces;

import java.util.EventListener;

/**
 * Model listener.
 */
public interface NetworkListener extends EventListener {

    /**
     * Notify this listener of a model cleared event.
     *
     * @param e event
     */
    void modelCleared(NetworkEvent e);

    /**
     * Notify this listener of a Neuron added event.
     *
     * @param e event
     */
    void neuronAdded(NetworkEvent e);

    /**
     * Notify this listener of a Neuron removed event.
     *
     * @param e event
     */
    void neuronRemoved(NetworkEvent e);
}