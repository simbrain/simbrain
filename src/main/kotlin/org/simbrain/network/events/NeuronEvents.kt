package org.simbrain.network.events

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * @see Event
 */
class NeuronEvents(val neuron: Neuron) : LocationEvents(neuron) {

    fun onActivationChange(handler: BiConsumer<Double, Double>) = "ActivationChange".itemChangedEvent(handler)
    fun fireActivationChange(old: Double, new: Double) = "ActivationChange"(old = old, new = new)

    fun onSpiked(handler: BiConsumer<Boolean, Boolean>) = "Spiked".itemChangedEvent(handler)
    fun fireSpiked(old: Boolean, new: Boolean) = "Spiked"(old = old, new = new)

    override fun onLocationChange(handler: Runnable) = "LocationChange".event(handler)
    override fun fireLocationChange() = "LocationChange"()

    fun onLabelChange(handler: Runnable) = "LabelChange".event(handler)
    fun fireLabelChange() = "LabelChange"()

    fun onColorChange(handler: Runnable) = "ColorChange".event(handler)
    fun fireColorChange() = "ColorChange"()

    fun onClampedChange(handler: BiConsumer<Boolean, Boolean>) = "ClampedChange".itemChangedEvent(handler)
    fun fireClampedChange(old: Boolean?, new: Boolean) = "ClampedChange"(old = old, new = new)

    fun onUpdateRuleChange(handler: BiConsumer<NeuronUpdateRule, NeuronUpdateRule>) = "UpdateRuleChange".itemChangedEvent(handler)
    fun fireUpdateRuleChange(old: NeuronUpdateRule?, new: NeuronUpdateRule) = "UpdateRuleChange"(old = old, new = new)

}