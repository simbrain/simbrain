package org.simbrain.network.events

import org.simbrain.network.core.SynapseUpdateRule
import org.simbrain.util.Events2

/**
 * See [Events2]
 */
class SynapseEvents2: NetworkModelEvents2() {
    val strengthUpdated = NoArgEvent()
    val learningRuleUpdated = ChangedEvent<SynapseUpdateRule>()
    val visbilityChanged = ChangedEvent<Boolean>()
}