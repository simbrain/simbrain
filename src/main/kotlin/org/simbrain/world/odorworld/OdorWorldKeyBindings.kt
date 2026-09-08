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
 * the key is held, cell after cell; continuous entities get a held-key speed and turn state. Both are carried by
 * the panel's movement timer while the world is stopped and by world updates while it runs.
 */
fun OdorWorldPanel.addKeyBindings() {
    canvas.apply {
        bind(CmdOrCtrl + Shift + 'B') {
            debugToolTips()
        }

        fun OdorWorldEntity.isGrid() = movementMode == MovementMode.GRID

        fun pressGrid(direction: GridDirection): Boolean {
            firstSelectedRotatingEntity?.takeIf { it.isGrid() } ?: return false
            pressGridDirection(direction)
            return true
        }

        fun releaseGrid(direction: GridDirection): Boolean {
            releaseGridDirection(direction)
            return firstSelectedRotatingEntity?.isGrid() == true
        }

        // Manual Forward Motion
        bind("pressed W", "pressed UP") {
            setManualMovementKeyState("w", true)
            if (pressGrid(GridDirection.NORTH)) return@bind
            firstSelectedRotatingEntity?.let {
                it.manualMovement.speed = 1.0
            }
        }
        bind("released W", "released UP") {
            setManualMovementKeyState("w", false)
            if (releaseGrid(GridDirection.NORTH)) return@bind
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
            setManualMovementKeyState("s", true)
            if (pressGrid(GridDirection.SOUTH)) return@bind
            firstSelectedRotatingEntity?.let {
                it.manualMovement.speed = -1.0
            }
        }
        bind("released S", "released DOWN") {
            setManualMovementKeyState("s", false)
            if (releaseGrid(GridDirection.SOUTH)) return@bind
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
            setManualMovementKeyState("a", true)
            if (pressGrid(GridDirection.WEST)) return@bind
            firstSelectedRotatingEntity?.manualMovement?.turnLeft()
        }
        bind("released A", "released LEFT") {
            setManualMovementKeyState("a", false)
            if (releaseGrid(GridDirection.WEST)) return@bind
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
            setManualMovementKeyState("d", true)
            if (pressGrid(GridDirection.EAST)) return@bind
            firstSelectedRotatingEntity?.manualMovement?.turnRight()
        }
        bind("released D", "released RIGHT") {
            setManualMovementKeyState("d", false)
            if (releaseGrid(GridDirection.EAST)) return@bind
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