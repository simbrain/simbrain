package org.simbrain.network.events

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.util.Event
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * @see Event
 */
class NeuronEvents(val neuron: Neuron) : LocationEvents(neuron) {

    fun onActivationChange(handler: BiConsumer<Double, Double>) = GlobalScope.launch(Dispatchers.Main) {
        "ActivationChange".itemChangedEvent(handler)
    }
    fun fireActivationChange(old: Double, new: Double) = "ActivationChange"(old = old, new = new)

    fun onSpiked(handler: Consumer<Boolean>) = "Spiked".itemAddedEvent(handler)
    fun fireSpiked(spiked: Boolean ) = "Spiked"(new = spiked)

    fun onColorChange(handler: Runnable) = "ColorChange".event(handler)
    fun fireColorChange() = "ColorChange"()

    fun onUpdateRuleChange(handler: BiConsumer<NeuronUpdateRule, NeuronUpdateRule>) = "UpdateRuleChange".itemChangedEvent(handler)
    fun fireUpdateRuleChange(old: NeuronUpdateRule?, new: NeuronUpdateRule) = "UpdateRuleChange"(old = old, new = new)

}