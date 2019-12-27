package org.simbrain.network.events

import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseUpdateRule
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

class SynapseEvents(val synapse: Synapse) : Event(PropertyChangeSupport(synapse)) {

    fun onDelete(handler: Consumer<Synapse>) = "Delete".itemRemovedEvent(handler)
    fun fireDelete() = "Delete"(old = synapse)

    fun onStrengthUpdate(handler: Runnable) = "StrengthUpdate".event(handler)
    fun fireStrengthUpdate() = "StrengthUpdate"()

    fun onLearningRuleUpdate(handler: BiConsumer<SynapseUpdateRule, SynapseUpdateRule>)
            = "LearningRuleUpdate".itemChangedEvent(handler)
    fun fireLearningRuleUpdate(old: SynapseUpdateRule, new: SynapseUpdateRule)
            = "LearningRuleUpdate"(old = old, new = new)

}