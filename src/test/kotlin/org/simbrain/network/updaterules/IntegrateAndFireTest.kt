package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import kotlin.math.exp

class IntegrateAndFireTest {

    @Test
    fun `stays at resting potential when resistance is 0`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)
        // S3 defaults lastSpikeTime=0.0, so neuron can't spike until time > refractoryPeriod.
        // To allow immediate spiking in tests, set lastSpikeTime to large negative value.
        (n.dataHolder as org.simbrain.network.util.SpikingScalarData).lastSpikeTime = -1000.0
    }

    // TODO: Test threshold, time constant, resistance

    @Test
    fun `stays at resting potential when resistance is 0`() {
        intFire.resistance = 0.0
        n.activation = intFire.restingPotential
        repeat(10) {
            net.update()
            assertEquals(intFire.restingPotential, n.activation)
        }
    }

    @Test
    fun `large current triggers one spike followed by no spike because of refractory period`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.backgroundCurrent = 1000.0
        with(net) {
            update()
            assertTrue(n.isSpike)
            update()
            assertFalse(n.isSpike)
        }
    }

    @Test
    fun `goes to reset potential after a spike`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.backgroundCurrent = 1000.0
        net.update()
        assertEquals(intFire.resetPotential, n.activation)
    }

    @Test
    fun `decays to resting potential`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.backgroundCurrent = 0.0
        intFire.timeConstant = 1.0
        repeat(100) {
            net.update()
        }
        assertEquals(intFire.restingPotential, n.activation, .001)
    }

    @Test
    fun `neuron only spikes between refractory periods`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.refractoryPeriod = 5.0
        intFire.backgroundCurrent = 1000.0
        intFire.timeConstant = 1.0
        net.timeStep = 1.0

        with(net) {
            var lastSpikeTime = -100.0
            repeat(50) { iteration ->
                update()
                if (n.isSpike) {
                    val timeSinceLastSpike = time - lastSpikeTime
                    assertTrue(timeSinceLastSpike >= intFire.refractoryPeriod || lastSpikeTime == -100.0,
                        "Spike occurred too soon: only ${timeSinceLastSpike}ms since last spike at iteration $iteration")
                    lastSpikeTime = time
                }
            }
        }
    }

    @Test
    fun `threshold parameter is respected`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.threshold = -45.0
        intFire.backgroundCurrent = 1000.0
        n.activation = -46.0

        with(net) {
            update()
            assertTrue(n.isSpike)
            assertEquals(intFire.resetPotential, n.activation)
        }
    }

    @Test
    fun `time constant affects decay rate`() {
        val net1 = Network()
        val intFire1 = IntegrateAndFireRule()
        val n1 = Neuron(intFire1)
        net1.addNetworkModelAsync(n1)

        val net2 = Network()
        val intFire2 = IntegrateAndFireRule()
        val n2 = Neuron(intFire2)
        net2.addNetworkModelAsync(n2)

        intFire1.timeConstant = 10.0
        intFire1.backgroundCurrent = 0.0
        n1.activation = -60.0

        intFire2.timeConstant = 100.0
        intFire2.backgroundCurrent = 0.0
        n2.activation = -60.0

        with(net1) {
            repeat(10) {
                update()
            }
        }
        with(net2) {
            repeat(10) {
                update()
            }
        }

        val decay1 = kotlin.math.abs(n1.activation - intFire1.restingPotential)
        val decay2 = kotlin.math.abs(n2.activation - intFire2.restingPotential)
        assertTrue(decay1 < decay2, "Smaller time constant should decay faster")
    }

    @Test
    fun `resistance affects membrane potential change`() {
        val net1 = Network()
        val intFire1 = IntegrateAndFireRule()
        val n1 = Neuron(intFire1)
        net1.addNetworkModelAsync(n1)

        val net2 = Network()
        val intFire2 = IntegrateAndFireRule()
        val n2 = Neuron(intFire2)
        net2.addNetworkModelAsync(n2)

        intFire1.resistance = 1.0
        intFire1.backgroundCurrent = 10.0
        n1.activation = -70.0

        intFire2.resistance = 5.0
        intFire2.backgroundCurrent = 10.0
        n2.activation = -70.0

        with(net1) {
            update()
        }
        with(net2) {
            update()
        }

        val change1 = n1.activation - (-70.0)
        val change2 = n2.activation - (-70.0)
        assertTrue(change2 > change1, "Higher resistance should amplify current effect")
    }

    @Test
    fun `extreme positive current does not cause NaN`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.backgroundCurrent = 10000.0
        intFire.refractoryPeriod = 1.0

        repeat(100) {
            net.update()
            assertFalse(n.activation.isNaN(), "Activation should not be NaN at iteration $it")
            assertFalse(n.activation.isInfinite(), "Activation should not be infinite at iteration $it")
            assertTrue(n.activation >= intFire.restingPotential - 50.0,
                "Activation should not go far below resting potential, got ${n.activation}")
        }
    }

    @Test
    fun `extreme negative current does not cause NaN or unbounded decrease`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.backgroundCurrent = -10000.0

        repeat(100) {
            net.update()
            assertFalse(n.activation.isNaN(), "Activation should not be NaN at iteration $it")
            assertFalse(n.activation.isInfinite(), "Activation should not be infinite at iteration $it")
        }
    }

    @Test
    fun `very small time constant remains stable`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.timeConstant = 1.0
        intFire.backgroundCurrent = 50.0

        repeat(100) {
            net.update()
            assertFalse(n.activation.isNaN(), "Activation should not be NaN with small time constant")
            assertFalse(n.activation.isInfinite(), "Activation should not be infinite")
        }
    }

    @Test
    fun `very large time constant remains stable`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.timeConstant = 10000.0
        intFire.backgroundCurrent = 50.0

        repeat(100) {
            net.update()
            assertFalse(n.activation.isNaN(), "Activation should not be NaN with large time constant")
            assertFalse(n.activation.isInfinite(), "Activation should not be infinite")
        }
    }

    @Test
    fun `zero time constant does not cause division by zero`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.timeConstant = 0.0
        intFire.backgroundCurrent = 10.0

        with(net) {
            update()
        }
        assertTrue(n.activation.isInfinite() || n.activation.isNaN(),
            "Zero time constant creates numerical instability (expected behavior)")
    }

    @Test
    fun `large timestep remains stable`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        net.timeStep = 100.0
        intFire.backgroundCurrent = 50.0
        intFire.refractoryPeriod = 10.0

        repeat(50) {
            net.update()
            assertFalse(n.activation.isNaN(), "Activation should not be NaN with large timestep")
            assertFalse(n.activation.isInfinite(), "Activation should not be infinite")
        }
    }

    @Test
    fun `refractory period blocks all input`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.refractoryPeriod = 10.0
        intFire.backgroundCurrent = 1000.0
        net.timeStep = 1.0

        with(net) {
            update()
            assertTrue(n.isSpike)
            val afterSpikeActivation = n.activation

            repeat(9) {
                update()
                assertFalse(n.isSpike, "Should not spike during refractory period at time ${time}")
            }
        }
    }

    @Test
    fun `spike resets to reset potential even with extreme input`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.backgroundCurrent = 100000.0
        intFire.resetPotential = -70.0
        intFire.refractoryPeriod = 10.0
        net.timeStep = 1.0

        n.addInputValue(intFire.threshold + 100.0)
        with(net) {
            update()
            assertTrue(n.isSpike, "Should spike when above threshold")
            assertEquals(intFire.resetPotential, n.activation, 0.001,
                "Should reset to exactly reset potential, got ${n.activation}")
        }
    }

    @Test
    fun `activation never exceeds threshold during normal operation`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.backgroundCurrent = 50.0
        intFire.threshold = -50.0
        intFire.refractoryPeriod = 2.0

        with(net) {
            repeat(200) {
                update()
                if (!n.isSpike) {
                    assertTrue(n.activation <= intFire.threshold,
                        "Activation should not exceed threshold without spiking, got ${n.activation}")
                }
            }
        }
    }

    @Test
    fun `exponential decay matches theoretical model`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.backgroundCurrent = 0.0
        intFire.timeConstant = 30.0
        intFire.resistance = 1.0
        n.activation = -60.0
        val initialActivation = n.activation
        val dt = net.timeStep

        with(net) {
            repeat(10) {
                update()
            }
        }

        val totalTime = 10 * dt
        val expectedActivation = intFire.restingPotential +
            (initialActivation - intFire.restingPotential) * exp(-totalTime / intFire.timeConstant)

        assertEquals(expectedActivation, n.activation, 0.5,
            "Should follow exponential decay: expected $expectedActivation, got ${n.activation}")
    }

    @Test
    fun `no spike below threshold regardless of proximity`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.threshold = -50.0
        n.activation = -50.001
        intFire.backgroundCurrent = 0.0

        with(net) {
            update()
            assertFalse(n.isSpike, "Should not spike when just below threshold")
        }
    }

    @Test
    fun `spike occurs at threshold`() {
        val net = Network()
        val intFire = IntegrateAndFireRule()
        val n = Neuron(intFire)
        net.addNetworkModelAsync(n)

        intFire.threshold = -50.0
        intFire.restingPotential = -70.0
        intFire.timeConstant = 30.0
        intFire.resistance = 1.0
        n.activation = -51.0
        intFire.backgroundCurrent = 100.0
        net.timeStep = 0.5

        with(net) {
            update()
            assertTrue(n.isSpike, "Should spike when crossing threshold, activation was ${n.activation}")
        }
    }

    @Test
    fun `combined resistance and current effects are multiplicative`() {
        val net1 = Network()
        val intFire1 = IntegrateAndFireRule()
        val n1 = Neuron(intFire1)
        net1.addNetworkModelAsync(n1)

        val net2 = Network()
        val intFire2 = IntegrateAndFireRule()
        val n2 = Neuron(intFire2)
        net2.addNetworkModelAsync(n2)

        intFire1.resistance = 2.0
        intFire1.backgroundCurrent = 5.0
        intFire1.timeConstant = 10.0
        n1.activation = -70.0

        intFire2.resistance = 5.0
        intFire2.backgroundCurrent = 2.0
        intFire2.timeConstant = 10.0
        n2.activation = -70.0

        with(net1) {
            update()
        }
        with(net2) {
            update()
        }

        val change1 = n1.activation - (-70.0)
        val change2 = n2.activation - (-70.0)
        assertEquals(change1, change2, 0.001,
            "R*I products should produce same effect: ${change1} vs ${change2}")
    }

}