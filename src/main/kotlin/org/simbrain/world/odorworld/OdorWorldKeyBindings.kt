/**
 * Standalone key bindings on the odor world canvas: manual driving of the selected agent and two developer debugging
 * views. Shortcuts that belong to a menu action are declared on the action in OdorWorldActions.kt instead.
 */
package org.simbrain.world.odorworld

import org.simbrain.util.CmdOrCtrl
import org.simbrain.util.Shift
import org.simbrain.util.bind
import org.simbrain.world.odorworld.entities.MovementMode
import org.simbrain.world.odorworld.entities.OdorWorldEntity

/**
 * Manual keys drive grid-mode entities in absolute cardinal directions (up is north, left is west) for as long as
 * the key is held, cell after cell; continuous entities keep the held-key speed and turn state managed by the
 * panel's movement timer.
 */
fun OdorWorldPanel.addKeyBindings() {
    canvas.apply {
        bind(CmdOrCtrl + Shift + 'B') {
            debugToolTips()
        }

        fun OdorWorldEntity.isGrid() = movementMode == MovementMode.GRID

        fun pressGrid(direction: GridDirection): Boolean {
            val entity = firstSelectedRotatingEntity?.takeIf { it.isGrid() } ?: return false
            pressGridDirection(direction)
            return true
        }

        // Manual Forward Motion
        bind("pressed W", "pressed UP") {
            if (pressGrid(GridDirection.NORTH)) return@bind
            setManualMovementKeyState("w", true)
            firstSelectedRotatingEntity?.let {
                it.manualMovement.speed = 1.0
            }
        }
        bind("released W", "released UP") {
            releaseGridDirection(GridDirection.NORTH)
            setManualMovementKeyState("w", false)
            firstSelectedRotatingEntity?.let { entity ->
                // case where w and s are both being pressed
                if (getManualMovementState("s")) {
                    entity.manualMovement.speed = -1.0
                } else {
                    entity.manualMovement.speed = 0.0
                }
            }
        }

        // Manual Backward Motion
        bind("pressed S", "pressed DOWN") {
            if (pressGrid(GridDirection.SOUTH)) return@bind
            setManualMovementKeyState("s", true)
            firstSelectedRotatingEntity?.let {
                it.manualMovement.speed = -1.0
            }
        }
        bind("released S", "released DOWN") {
            releaseGridDirection(GridDirection.SOUTH)
            setManualMovementKeyState("s", false)
            firstSelectedRotatingEntity?.let { entity ->
                // case where w and s are both being pressed
                if (getManualMovementState("w")) {
                    entity.manualMovement.speed = 1.0
                } else {
                    entity.manualMovement.speed = 0.0
                }
            }
        }

        // Manual Left Turn
        bind("pressed A", "pressed LEFT") {
            if (pressGrid(GridDirection.WEST)) return@bind
            setManualMovementKeyState("a", true)
            firstSelectedRotatingEntity?.manualMovement?.turnLeft()
        }
        bind("released A", "released LEFT") {
            releaseGridDirection(GridDirection.WEST)
            setManualMovementKeyState("a", false)
            firstSelectedRotatingEntity?.let { entity ->
                // case where a and d are both being pressed
                if (getManualMovementState("d")) {
                    entity.manualMovement.turnRight()
                } else {
                    entity.manualMovement.stopTurning()
                }
            }
        }

        // Manual Right Turn
        bind("pressed D", "pressed RIGHT") {
            if (pressGrid(GridDirection.EAST)) return@bind
            setManualMovementKeyState("d", true)
            firstSelectedRotatingEntity?.manualMovement?.turnRight()
        }
        bind("released D", "released RIGHT") {
            releaseGridDirection(GridDirection.EAST)
            setManualMovementKeyState("d", false)
            firstSelectedRotatingEntity?.let { entity ->
                // case where a and d are both being pressed
                if (getManualMovementState("a")) {
                    entity.manualMovement.turnLeft()
                } else {
                    entity.manualMovement.stopTurning()
                }
            }
        }

        bind(CmdOrCtrl + Shift + 'P') {
            showPNodeDebugger()
        }
    }
}