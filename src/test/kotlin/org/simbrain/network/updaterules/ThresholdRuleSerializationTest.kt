/**
 * Checks threshold-rule behavior and compatibility when loading saved networks.
 */
package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream

class ThresholdRuleSerializationTest {
    @Test
    fun `load network with former binary rule name and save with threshold name`() {
        val network = Network()
        network.addNetworkModelAsync(Neuron(ThresholdRule(-2.0, 3.0, 0.7)).apply { label = "threshold" })
        val xml = getNetworkXStream().toXML(network)
        assertTrue(xml.contains("org.simbrain.network.updaterules.ThresholdRule"))
        val legacyXml = xml.replace("org.simbrain.network.updaterules.ThresholdRule", "org.simbrain.network.updaterules.BinaryRule")
        val restored = getNetworkXStream().fromXML(legacyXml) as Network
        val neuron = restored.getModelByLabel(Neuron::class.java, "threshold")
        val rule = neuron.updateRule as ThresholdRule
        assertEquals(0.7, rule.threshold)
        assertEquals(-2.0, rule.lowerBound)
        assertEquals(3.0, rule.upperBound)
        assertEquals(-2.0, rule.thresholdRule(0.7))
        assertEquals(3.0, rule.thresholdRule(0.8))
        val saved = getNetworkXStream().toXML(restored)
        assertFalse(saved.contains("BinaryRule"))
        val reloaded = getNetworkXStream().fromXML(saved) as Network
        assertInstanceOf(ThresholdRule::class.java, reloaded.getModelByLabel(Neuron::class.java, "threshold").updateRule)
    }
}
