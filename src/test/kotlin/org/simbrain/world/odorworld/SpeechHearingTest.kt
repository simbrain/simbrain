package org.simbrain.world.odorworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.util.point
import org.simbrain.world.odorworld.effectors.Speech
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.Hearing

class SpeechHearingTest {

    val world = OdorWorld()
    val talker = OdorWorldEntity(world).apply{
        world.addEntity(this)
    }
    val listener = OdorWorldEntity(world).apply {
        location = point(50,50)
        world.addEntity(this)
    }
    val speechEffector = Speech("Test", 0.0).also {
        talker.addEffector(it)
    }
    val hearingSensor = Hearing("Test", 1.0).also {
        listener.addSensor(it)
    }

    @Test
    fun `test basic speaking and hearing`() = runBlocking {

        hearingSensor.outputAmount = 2.0 // also testing a non-default output amount
        speechEffector.amount = 0.0 // Speech effector not activated
        world.update()
        assertEquals(false, hearingSensor.isActivated)
        assertEquals(0.0, hearingSensor.value)
        speechEffector.amount = 1.0 // Speech effector activated
        world.update()
        assertEquals(true, hearingSensor.isActivated)
        assertEquals(2.0, hearingSensor.value)

    }

    @Test
    fun `test no self-hearing`() = runBlocking {
        speechEffector.amount = 1.0
        talker.addSensor(Hearing("Test", 1.0))
        val selfHearing = talker.sensors[0] as Hearing
        world.update()
        assertEquals(false, selfHearing.isActivated)
        assertEquals(0.0, selfHearing.value)
        assertEquals(true, hearingSensor.isActivated)
        assertEquals(1.0, hearingSensor.value)
    }

    @Test
    fun `test speech signal dispersion`() = runBlocking {
        // Hearing sensor should not activate because the other sensor is more than
        // 10 pixels away
        speechEffector.decayFunction.dispersion = 10.0
        speechEffector.amount = 1.0
        world.update()
        assertEquals(false, hearingSensor.isActivated)
        assertEquals(0.0, hearingSensor.value)

        // Now it should work
        speechEffector.decayFunction.dispersion = 100.0
        speechEffector.amount = 1.0
        world.update()
        assertEquals(true, hearingSensor.isActivated)
        assertEquals(1.0, hearingSensor.value)
    }

    @Test
    fun `test lingertime`() = runBlocking {
        speechEffector.amount = 1.0
        hearingSensor.lingerTime = 2
        world.update()
        assertEquals(true, hearingSensor.isActivated)
        assertEquals(1.0, hearingSensor.value)
        world.update()
        assertEquals(true, hearingSensor.isActivated)
        assertEquals(1.0, hearingSensor.value)
        world.update()
        assertEquals(false, hearingSensor.isActivated)
        assertEquals(0.0, hearingSensor.value)
    }

    }