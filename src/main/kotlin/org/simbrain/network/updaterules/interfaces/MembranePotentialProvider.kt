/**
 * Capability interface for update rules that maintain a membrane potential, exposing it for electrical
 * connections such as gap junctions, which need an internal voltage rather than a squashed activation.
 */
package org.simbrain.network.updaterules.interfaces

import org.simbrain.network.core.Neuron

interface MembranePotentialProvider {

    /**
     * The neuron's membrane potential. Rules whose activation is itself the membrane voltage (most spiking
     * rules) return the activation; rules that squash an internal state expose that state.
     */
    fun membranePotential(neuron: Neuron): Double
}
