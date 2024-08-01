package org.simbrain.network.spikeresponders

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.updaterules.SpikingThresholdRule
import org.simbrain.util.getSimbrainXStream

class SpikeResponderTest {

    val net = Network()
    val n1 = Neuron() // Input
    val n2 = Neuron(SpikingThresholdRule()) // Spiking neuron
    val n3 = Neuron().also { it.upperBound = 10.0 } // receive spike response
    val s1 = Synapse(n1, n2)
    val s2 = Synapse(n2, n3) // This one has the spike responder

    init {
        net.addNetworkModels(n1, n2, n3, s1, s2)
    }

    @Test
    fun `responder data is copied correctly`() {
        val step = StepResponder()
        val newResponder: StepResponder = step.copy() as StepResponder
        assertEquals(step.responseDuration, newResponder.responseDuration)
        assertEquals(step.responseHeight, newResponder.responseHeight)
    }

    @Test
    fun `test NonResponder`() {
        val nr = NonResponder()
        s2.spikeResponder = nr
        s2.strength = .7
        n1.activation = 1.0
        net.update()
        net.update()
        // Just passes the weight through
        assertEquals(.7, n3.activation)
    }

    /**
     * Should "fire" at responseHeight (2.0) for responseDuration (3)
     */
    @Test
    fun `step responder produces correct height and duration `() {

        val step = StepResponder()
        step.responseHeight = .75
        step.responseDuration = 3
        s2.spikeResponder = step

        n1.activation = 1.0
        net.update() // First update propagates from n1 to n2, no spike response yet
        assertEquals(0.0, s2.psr)
        assertEquals(0.0, n3.activation)
        net.update()
        assertEquals(step.responseHeight, s2.psr)
        assertEquals(step.responseHeight, n3.activation)
        net.update()
        assertEquals(step.responseHeight, s2.psr)
        assertEquals(step.responseHeight, n3.activation)
        net.update()
        assertEquals(step.responseHeight, s2.psr)
        assertEquals(step.responseHeight, n3.activation)
        net.update()
        assertEquals(0.0, s2.psr)
        assertEquals(0.0, n3.activation)
    }

    @Test
    fun `test jump and decay`() {
        val jd = JumpAndDecay()
        jd.jumpHeight = 4.0
        jd.baseLine = 2.0
        jd.timeConstant = .15
        s2.spikeResponder = jd
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(4.0, n3.activation)
        repeat(10) {
            net.update()
        }
        assertEquals(2.0, n3.activation, .1)
    }

    @Test
    fun `test jump and decay with negative weight`() {
        val jd = JumpAndDecay()
        s2.strength = -.5
        s2.spikeResponder = jd
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(-.5, n3.activation)
        jd.timeConstant = .15
        repeat(10) {
            net.update()
        }
        assertEquals(0.0, n3.activation, .1)
    }

    @Test
    fun `test probabilistic response`() {
        val pr = StepResponder()
        pr.responseDuration = 1
        pr.spikeProbability = 1.0
        s2.spikeResponder = pr
        s2.strength = .5
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(.5, n3.activation)
        pr.spikeProbability = 0.0
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(0.0, n3.activation)
    }

    @Test
    fun `test probabalistic responder with negative weight`() {
        val pr = JumpAndDecay()
        pr.spikeProbability = 1.0
        s2.spikeResponder = pr
        s2.strength = -.5
        n1.activation = 1.0
        net.update()
        net.update()
        assertEquals(-.5, n3.activation)
    }

    @Test
    fun `test rise and decay`() {
        val rad = RiseAndDecay()
        s2.spikeResponder = rad
        n1.activation = 1.0
        net.update()
        net.update()
        println(s2)
        println(n3)
        net.update()
        println(s2)
        println(n3)
        net.update()
        println(s2)
        println(n3)
        net.update()
        println(s2)
        println(n3)
        net.update()
        println(s2)
        println(n3)
    }

    @Test
    fun `test convolved jump and decay`() {
        val cjd = ConvolvedJumpAndDecay()
        s2.spikeResponder = cjd
        s2.strength = .5
        n1.activation = 1.0
        n1.clamped = true
        net.update()
        net.update()
        assertEquals(.5, n3.activation)
        net.update()
        assertEquals(1.0, n3.activation)
        net.update()
        assertEquals(1.5, n3.activation)
        net.update()
        assertEquals(2.0, n3.activation)
        n1.clamped = false
        cjd.baseLine = 0.2
        cjd.timeConstant = .1 // decay quick
        repeat(10) {
            net.update()
        }
        assertEquals(0.2, n3.activation)
    }

    @Test
    fun `test UDF`() {
        val udf = UDF()
        s2.spikeResponder = udf
        n1.activation = 1.0
        net.update()
        net.update()
        println(s2)
        println(n3)
        net.update()
        println(s2)
        println(n3)
        net.update()
        println(s2)
        println(n3)
        net.update()
        println(s2)
        println(n3)
        net.update()
        println(s2)
        println(n3)
    }

    @Test
    fun `test udf xml representation`() {
        val udf = UDF().apply {
            U = 0.5
            D = 1100.0
            F = 50.0
        }
        s2.spikeResponder = udf
        n1.activation = 1.0
        net.update()

        val xml = getSimbrainXStream().toXML(s2)

        val synapse = getSimbrainXStream().fromXML(xml) as Synapse
        val deserializedUdf = synapse.spikeResponder as UDF

        assertEquals(udf.U, deserializedUdf.U)
        assertEquals(udf.D, deserializedUdf.D)
        assertEquals(udf.F, deserializedUdf.F)

    }
}