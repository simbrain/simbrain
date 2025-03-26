package org.simbrain.world.odorworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.world.odorworld.effectors.Speech
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.Hearing

class SpeechHearingTest {

    @Test
    fun `test hearing sensor`() = runBlocking {
        val world = OdorWorld()

        val talker = OdorWorldEntity(world)
        world.addEntity(talker)
        talker.addEffector(Speech("Test", 0.0))

        val listener = OdorWorldEntity(world)
        world.addEntity(listener)
        listener.addSensor(Hearing("Test", 1.0))

        val speechEffetor = talker.effectors[0] as Speech
        val hearingSensor = listener.sensors[0] as Hearing

        hearingSensor.outputAmount = 2.0
        speechEffetor.amount = 0.0
        world.update()
        assertEquals(false, hearingSensor.isActivated)
        assertEquals(0.0, hearingSensor.value)
        speechEffetor.amount = 1.0
        world.update()
        assertEquals(true, hearingSensor.isActivated)
        assertEquals(2.0, hearingSensor.value)

        // Check no self-hearing
        speechEffetor.amount = 1.0
        talker.addSensor(Hearing("Test", 1.0))
        val selfHearing = talker.sensors[0] as Hearing
        world.update()
        assertEquals(false, selfHearing.isActivated)
        assertEquals(0.0, selfHearing.value)
        assertEquals(true, hearingSensor.isActivated)
        assertEquals(2.0, hearingSensor.value)

    }
    
}