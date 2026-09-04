package org.simbrain.world.odorworld

import kotlinx.coroutines.launch
import org.simbrain.util.Ctrl
import org.simbrain.util.bind
import org.simbrain.world.odorworld.entities.MovementMode
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.event.KeyEvent

/**
 * Manual keys drive grid-mode entities one cell or one quarter turn per press; continuous entities keep the
 * held-key speed and turn state managed by the panel's movement timer.
 */
fun OdorWorldPanel.addKeyBindings() {
    canvas.apply {
        // Debug tooltip test
        bind("B") {
            debugToolTips()
        }

        fun OdorWorldEntity.isGrid() = movementMode == MovementMode.GRID

        // Manual Forward Motion
        bind("pressed W", "pressed UP") {
            firstSelectedRotatingEntity?.takeIf { it.isGrid() }?.let { entity ->
                world.launch { entity.moveOneCell(entity.facingDirection, face = false) }
                return@bind
            }
            setManualMovementKeyState("w", true)
            firstSelectedRotatingEntity?.let {
                it.manualMovement.speed = 1.0
            }
        }
        bind("released W", "released UP") {
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
            firstSelectedRotatingEntity?.takeIf { it.isGrid() }?.let { entity ->
                world.launch { entity.moveOneCell(entity.facingDirection.opposite, face = false) }
                return@bind
            }
            setManualMovementKeyState("s", true)
            firstSelectedRotatingEntity?.let {
                it.manualMovement.speed = -1.0
            }
        }
        bind("released S", "released DOWN") {
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
            firstSelectedRotatingEntity?.takeIf { it.isGrid() }?.let { entity ->
                entity.turn(90.0)
                return@bind
            }
            setManualMovementKeyState("a", true)
            firstSelectedRotatingEntity?.manualMovement?.turnLeft()
        }
        bind("released A", "released LEFT") {
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
            firstSelectedRotatingEntity?.takeIf { it.isGrid() }?.let { entity ->
                entity.turn(-90.0)
                return@bind
            }
            setManualMovementKeyState("d", true)
            firstSelectedRotatingEntity?.manualMovement?.turnRight()
        }
        bind("released D", "released RIGHT") {
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

        // Debug Piccolo
        bind(Ctrl + KeyEvent.VK_P) {
            showPNodeDebugger()
        }
    }
}