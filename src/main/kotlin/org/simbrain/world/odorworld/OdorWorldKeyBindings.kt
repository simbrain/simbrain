package org.simbrain.world.odorworld

import org.simbrain.util.Ctrl
import org.simbrain.util.bind
import java.awt.event.KeyEvent

fun OdorWorldPanel.addKeyBindings() {
    canvas.apply {
        // Debug tooltip test
        bind("B") {
            debugToolTips()
        }

        // Manual Forward Motion
        bind("pressed W", "pressed UP") {
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