package org.simbrain.network.events

import org.simbrain.network.learningrules.SynapseUpdateRule
import org.simbrain.util.FlowEvents

/**
 * See [FlowEvents]
 */
class SynapseEvents: NetworkModelEvents() {
    val strengthUpdated = NoArgEvent()
    val learningRuleUpdated = ChangedEvent<SynapseUpdateRule<*, *>>()
    val visbilityChanged = ChangedEvent<Boolean>()
    val colorPreferencesChanged = NoArgEvent()
    val locationChanged = NoArgEvent()
}