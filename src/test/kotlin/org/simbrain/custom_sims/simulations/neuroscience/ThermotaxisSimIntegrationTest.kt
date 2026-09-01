/**
 * Headless closed-loop check of the native thermotaxis wiring: temperature sensor coupled into AFD,
 * motor neurons coupled into turning effectors, and the worm behavior driving locomotion, all running
 * through ordinary workspace iteration with no hidden update action.
 */
package org.simbrain.custom_sims.simulations.neuroscience

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.AfdScalarData
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.couplings.ScaleOperation
import org.simbrain.workspace.couplings.getConsumer
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.TemperatureSensor
import org.simbrain.world.odorworld.sensors.ThermalGradient
import java.awt.geom.Point2D
import kotlin.math.PI
import kotlin.math.abs

class ThermotaxisSimIntegrationTest {

    private class Harness {
        val workspace = Workspace()
        val networkComponent = NetworkComponent("circuit", Network())
        val worldComponent = OdorWorldComponent("plate")
        val circuit = ThermotaxisNativeCircuit.build(networkComponent.network)
        val gradient = ThermalGradient()
        val world = worldComponent.world.apply {
            wrapAround = false
            isObjectsBlockMovement = false
            tileMap.updateMapSize(34, 24)
        }
        val worm = runBlocking { world.addEntity(world.width / 2.0, world.height / 2.0, EntityType.Nematode) }
        val sensor = TemperatureSensor().apply { gradient = this@Harness.gradient }
        val behavior = ThermotaxisWormBehavior().apply {
            gradient = this@Harness.gradient
            useEmpiricalTurns = false
        }

        init {
            worm.addSensor(sensor)
            val turningLeft = Turning(Turning.LEFT)
            val turningRight = Turning(Turning.RIGHT)
            worm.addEffector(turningLeft)
            worm.addEffector(turningRight)
            worm.behavior = behavior
            workspace.addWorkspaceComponent(networkComponent)
            workspace.addWorkspaceComponent(worldComponent)
            val scale = NEUROMUSCULAR_WEIGHT * 0.1 * 180.0 / PI
            with(workspace.couplingManager) {
                sensor.getProducer(sensor::currentValue) couple
                    circuit.afd.getConsumer(Neuron::setTemperatureInput)
                circuit.dmn.getProducer(circuit.dmn::activation) via ScaleOperation(scale) couple
                    turningLeft.getConsumer(Turning::setAmount)
                circuit.vmn.getProducer(circuit.vmn::activation) via ScaleOperation(scale) couple
                    turningRight.getConsumer(Turning::setAmount)
            }
            // Mirrors the sim's reset: prime AFD's history from the worm's actual temperature so the
            // first coupled sample is not the sensor's uninitialized 0 °C.
            sensor.update(worm)
            circuit.afd.setTemperatureInput(sensor.currentValue)
        }
    }

    @Test
    fun `the sensor reports the true plate temperature before the first coupling fires`() {
        val harness = Harness()

        assertEquals(
            17.0,
            harness.sensor.currentValue,
            1e-9,
            "an unread sensor would deliver 0 °C on the first iteration and prime AFD's history cold"
        )
    }

    @Test
    fun `temperature reaches AFD through the sensor coupling`() {
        val harness = Harness()

        harness.workspace.simpleIterate(5)

        val holder = harness.circuit.afd.dataHolder as AfdScalarData
        assertTrue(holder.primed, "the sensor coupling must prime AFD's temperature history")
        assertEquals(
            harness.gradient.temperatureAt(Point2D.Double(harness.world.width / 2.0, 0.0), harness.world),
            17.0,
            1e-9,
            "worm starts at the plate center"
        )
    }

    @Test
    fun `the worm crawls and the motor neurons steer its heading`() {
        val harness = Harness()
        val startX = harness.worm.x
        val startY = harness.worm.y
        harness.worm.heading = 90.0

        harness.workspace.simpleIterate(300)

        val moved = abs(harness.worm.x - startX) + abs(harness.worm.y - startY)
        assertTrue(moved > 1.0, "the behavior must move the worm, but it moved only $moved px")
        assertNotEquals(90.0, harness.worm.heading, "steering must adjust the heading")
        assertEquals(0.0, harness.worm.speed, 0.0, "translation is behavior-owned; entity speed stays zero")
    }

    @Test
    fun `circuit dynamics run under plain workspace iteration`() {
        val harness = Harness()

        harness.workspace.simpleIterate(100)

        val outputs = harness.circuit.interneurons.map { it.activation }
        assertTrue(outputs.all { it in 0.0..1.0 })
        assertTrue(abs(harness.circuit.cpg.activation) > 1e-6, "the CPG must oscillate")
    }
}
