package org.simbrain.network.events

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.util.Event
import java.util.function.BiConsumer

/**
 * @see Event
 */
class NeuronEvents(val neuron: Neuron) : LocationEvents(neuron) {

    fun onActivationChange(handler: BiConsumer<Double, Double>) = "ActivationChange".itemChangedEvent(handler)
    fun fireActivationChange(old: Double, new: Double) = "ActivationChange"(old = old, new = new)

    fun onSpiked(handler: BiConsumer<Boolean, Boolean>) = "Spiked".itemChangedEvent(handler)
    fun fireSpiked(old: Boolean, new: Boolean) = "Spiked"(old = old, new = new)

    fun onColorChange(handler: Runnable) = "ColorChange".event(handler)
    fun fireColorChange() = "ColorChange"()

    fun onUpdateRuleChange(handler: BiConsumer<NeuronUpdateRule, NeuronUpdateRule>) = "UpdateRuleChange".itemChangedEvent(handler)
    fun fireUpdateRuleChange(old: NeuronUpdateRule?, new: NeuronUpdateRule) = "UpdateRuleChange"(old = old, new = new)

}