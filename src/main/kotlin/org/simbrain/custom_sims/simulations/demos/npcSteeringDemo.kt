package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.addOdorWorldComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.behaviors.Evade
import org.simbrain.world.odorworld.behaviors.Pursue
import org.simbrain.world.odorworld.behaviors.Wander
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.fitWorldToFrameSize

val npcSteeringDemo = newSim {

    workspace.clearWorkspace()

    val odorWorldComponent = addOdorWorldComponent("NPC Steering Demo")
    val odorWorld = odorWorldComponent.world.apply {
        wrapAround = true
        isObjectsBlockMovement = true
    }

    // Mouse: pursues Swiss cheese at max speed 1.5
    odorWorld.addEntity(120, 200, EntityType.Mouse).apply {
        name = "Mouse"
        heading = 0.0
        showSteeringDebug = true
        behavior = Pursue().apply {
            targetType = EntityType.Swiss
            maxSpeed = 1.5
            visionRange = 400.0
        }
    }

    // Two Swiss cheeses: evade the Mouse at max speed 2.0
    odorWorld.addEntity(380, 120, EntityType.Swiss).apply {
        name = "Swiss A"
        showSteeringDebug = true
        behavior = Evade().apply {
            threatType = EntityType.Mouse
            maxSpeed = 2.0
            visionRange = 300.0
        }
    }
    odorWorld.addEntity(380, 320, EntityType.Swiss).apply {
        name = "Swiss B"
        showSteeringDebug = true
        behavior = Evade().apply {
            threatType = EntityType.Mouse
            maxSpeed = 2.0
            visionRange = 300.0
        }
    }

    // Wandering Fish — no target/threat, just roams around obstacles
    odorWorld.addEntity(220, 380, EntityType.Fish).apply {
        name = "Fish"
        showSteeringDebug = true
        behavior = Wander().apply {
            maxSpeed = 1.2
        }
    }

    // Static obstacles (no behavior). Their AABBs block movement and show up as
    // magenta hits in the steering debug overlay of nearby moving agents.
    odorWorld.addEntity(250, 220, EntityType.Pansy).apply { name = "Pansy" }
    odorWorld.addEntity(160, 320, EntityType.Tulip).apply { name = "Tulip" }

    addSidebarInfo(
        """
        # NPC Steering Demo

        A Mouse pursues two Swiss cheeses while they evade it. A Fish wanders
        independently. A Pansy and a Tulip sit still as obstacles. All four moving
        entities have **Show Steering Debug** turned on, so you can watch how each
        one is making decisions.

        # What to Do

        Press the play button. Watch the Mouse track down the cheeses, the cheeses
        flee in different directions, and the Fish meander around the obstacles
        without bumping them.

        # Reading the Debug Overlay

        Each agent draws three things every tick:

        - **Colored rays** from its center, one per candidate heading. They rotate
          with the agent so the ray pointing forward is always "ahead."
            - **Green** = a direction the behavior likes (positive score: target ahead, open path).
            - **Red** = a direction it dislikes (negative score: threat ahead, wall blocking).
            - **Yellow + thicker** = the chosen heading this tick.
            - Length scales with how strong the score is relative to the best alternative.
        - **Magenta dots** mark where the obstacle feeler ray hits a wall or another
          entity's AABB. The hit distance accounts for the agent's own width
          (Minkowski expansion), so the dot is where the agent's *edge* would touch.
        - **Status text** under the agent shows what it's doing, plus its intended
          vs actual movement:
            - `Pursue: 2 Swiss in range` / `Evade: 1 Mouse in range` / `Wander` —
              the active behavior and how many targets/threats it sees.
            - `speed: 1.50 → 1.50` — intended speed → actual speed after collision.
              When these diverge, the agent is being blocked.
            - `COLLIDED` (red) — collision shortened the move on at least one axis.
            - `STUCK (no progress)` (red) — intended speed > 0 but actual ≈ 0.
            - `— escape mode` appears when the agent has been stuck for a tick:
              ray density doubles and the candidate angles jitter so narrow gaps
              missed last tick get sampled this tick.

        # Things to Try

        - Drag a cheese close to the mouse — watch it react.
        - Drag the Pansy or Tulip into the cheese's escape path. The cheese should
          steer around it (you'll see magenta hits on its forward rays).
        - Open an entity's property dialog (right-click → Properties) and switch to
          the **Behavior** tab. Tweak `Max Speed`, `Vision Range`, `Num Rays`, or
          `Obstacle Avoidance Weight`, and watch the rays change in real time.

        # How It Works

        Each behavior uses **context steering**: every tick it samples N candidate
        headings around the agent, scores each by interest (toward target / desired
        heading) minus danger (toward threat / into wall or entity), and picks the
        best. The same kernel powers Pursue, Evade, and Wander — only the scoring
        function differs. See `world/odorworld/behaviors/`.
        """.trimIndent(),
        width = 350,
        initiallyOpened = true
    )

    withGui {
        place(odorWorldComponent) {
            location = point(0, 0)
            width = 700
            height = 600
        }
        odorWorldComponent.getDesktopComponentAs<OdorWorldDesktopComponent>().fitWorldToFrameSize()
    }
}
