package org.simbrain.network.events

import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseUpdateRule
import org.simbrain.util.Event
import java.util.function.BiConsumer

/**
 * @see Event
 */
class SynapseEvents(val synapse: Synapse) : NetworkModelEvents(synapse) {

    fun onStrengthUpdate(handler: Runnable) = "StrengthUpdate".event(handler)
    fun fireStrengthUpdate() = "StrengthUpdate"()

    fun onLearningRuleUpdate(handler: BiConsumer<SynapseUpdateRule, SynapseUpdateRule>)
            = "LearningRuleUpdate".itemChangedEvent(handler)
    fun fireLearningRuleUpdate(old: SynapseUpdateRule, new: SynapseUpdateRule)
            = "LearningRuleUpdate"(old = old, new = new)

    fun onVisibilityChanged(handler: BiConsumer<Boolean, Boolean>) = "VisibilityChanged".itemChangedEvent(handler)
    fun fireVisibilityChanged(old: Boolean, new: Boolean) = "VisibilityChanged"(old = old, new = new)

}