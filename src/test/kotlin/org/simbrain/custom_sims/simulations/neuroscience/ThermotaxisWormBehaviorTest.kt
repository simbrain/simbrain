/**
 * Behavior-level checks for [ThermotaxisWormBehavior]: a scripted empirical turn must own the heading
 * for its whole duration even when turning effectors nudge the entity between ticks, and the behavior
 * registers itself with the npc behavior type list so entity dialogs can display it.
 */
package org.simbrain.custom_sims.simulations.neuroscience

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.behaviors.npcBehaviorTypes
import org.simbrain.world.odorworld.entities.EntityType
import kotlin.math.PI
import kotlin.random.Random

class ThermotaxisWormBehaviorTest {

    @Test
    fun `effector steering does not leak into a scripted turn`() {
        val world = OdorWorldComponent("plate").world.apply {
            wrapAround = false
            tileMap.updateMapSize(34, 24)
        }
        val worm = runBlocking { world.addEntity(world.width / 2.0, world.height / 2.0, EntityType.Nematode) }
        val behavior = ThermotaxisWormBehavior()
        val seed = (1..100_000).first { candidate ->
            val turn = ThermotaxisTurnPolicy.select(17.0, 0.0, worm.heading * PI / 180.0, Random(candidate))
            turn != null && turn.durationSeconds > 0.35
        }
        behavior.turnRandom = Random(seed)

        behavior.update(worm)
        assertTrue(behavior.activeTurnLabel != null, "the seeded draw must start a turn")
        val turnHeading = worm.heading

        worm.heading = worm.heading + 30.0
        behavior.update(worm)

        assertEquals(turnHeading, worm.heading, 1e-9, "the scripted turn must own the heading")
    }

    @Test
    fun `the behavior registers itself in the npc behavior type list`() {
        ThermotaxisWormBehavior()

        assertTrue(ThermotaxisWormBehavior::class.java in npcBehaviorTypes)
    }
}
