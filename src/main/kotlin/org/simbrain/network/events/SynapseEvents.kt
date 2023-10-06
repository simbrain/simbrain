package org.simbrain.network.events

import org.simbrain.network.core.SynapseUpdateRule
import org.simbrain.util.Events

/**
 * See [Events]
 */
class SynapseEvents: NetworkModelEvents() {
    val strengthUpdated = NoArgEvent()
    val learningRuleUpdated = ChangedEvent<SynapseUpdateRule<*,*>>()
    val visbilityChanged = ChangedEvent<Boolean>()
    val colorPreferencesChanged = NoArgEvent()
}