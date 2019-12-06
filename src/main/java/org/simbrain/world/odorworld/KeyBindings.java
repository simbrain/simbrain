package org.simbrain.world.odorworld;

import org.piccolo2d.PCanvas;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Add key bindings to network panel. Controls many keyboard shortcuts. Bindings
 * not found here are in the action classes.
 */
public class KeyBindings {

    public static void addBindings(OdorWorldPanel worldPanel) {

        PCanvas canvas = worldPanel.getCanvas();
        OdorWorld world = worldPanel.getWorld();

        // Add / delete
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("P"), "addEntity");
        canvas.getActionMap().put("addEntity", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                world.addEntity();
                // TODO: Reuse network panel "click stream" logic
            }
        });

        canvas.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.SHIFT_MASK), "addAgent");
        canvas.getActionMap().put("addAgent", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                world.addAgent();
            }
        });

        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), "deleteSelection");
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), "deleteSelection");
        canvas.getActionMap().put("deleteSelection", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                worldPanel.deleteSelectedEntities();
            }
        });
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), "deleteSelection");
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), "deleteSelection");
        canvas.getActionMap().put("deleteSelection", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                worldPanel.deleteSelectedEntities();
            }
        });

        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("B"), "tooltipTest");
        canvas.getActionMap().put("tooltipTest", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                worldPanel.debugToolTips();
            }
        });

        // Example of getting press and release events
        // See https://docs.oracle.com/javase/8/docs/api/javax/swing/KeyStroke.html#getKeyStroke-java.lang.String-

        //
        // Manual Forward Motion
        //
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed W"), "start moving forward");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed UP"), "start moving forward");
        canvas.getActionMap().put("start moving forward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                worldPanel.setManualMovementState("w", true);
                OdorWorldEntity entity = worldPanel.getFirstSelectedRotatingEntity();
                if (entity != null) {
                    entity.setManualMode(true);
                    entity.goStraight();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released W"), "stop moving forward");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released UP"), "stop moving forward");
        canvas.getActionMap().put("stop moving forward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                worldPanel.setManualMovementState("w", false);
                OdorWorldEntity entity = worldPanel.getFirstSelectedRotatingEntity();
                if (entity != null) {
                    // case where w and s are both being pressed
                    if (worldPanel.getManualMovementState("s")) {
                        entity.goBackwards();
                    } else {
                        worldPanel.releaseManualMovement(entity);
                    }
                }
            }
        });

        //
        // Manual Backward Motion
        //
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed S"), "start moving backward");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed DOWN"), "start moving backward");
        canvas.getActionMap().put("start moving backward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                worldPanel.setManualMovementState("s", true);
                OdorWorldEntity entity = worldPanel.getFirstSelectedRotatingEntity();
                if (entity != null) {
                    entity.setManualMode(true);
                    entity.goBackwards();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released S"), "stop moving backward");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released DOWN"), "stop moving backward");
        canvas.getActionMap().put("stop moving backward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                worldPanel.setManualMovementState("s", false);
                OdorWorldEntity entity = worldPanel.getFirstSelectedRotatingEntity();
                if (entity != null) {
                    // case where w and s are both being pressed
                    if (worldPanel.getManualMovementState("w")) {
                        entity.goStraight();
                    } else {
                        worldPanel.releaseManualMovement(entity);
                    }
                }
            }
        });


        //
        // Manual Left Turn
        //
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed A"), "start turning left");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed LEFT"), "start turning left");
        canvas.getActionMap().put("start turning left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                worldPanel.setManualMovementState("a", true);
                OdorWorldEntity entity = worldPanel.getFirstSelectedRotatingEntity();
                if (entity != null) {
                    entity.setManualMode(true);
                    entity.turnLeft();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released A"), "stopTurningLeft");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released LEFT"), "stopTurningLeft");
        canvas.getActionMap().put("stopTurningLeft", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                worldPanel.setManualMovementState("a", false);
                OdorWorldEntity entity = worldPanel.getFirstSelectedRotatingEntity();
                if (entity != null) {
                    // case where a and d are both being pressed
                    if (worldPanel.getManualMovementState("d")) {
                        entity.turnRight();
                    } else {
                        entity.stopTurning();
                        worldPanel.releaseManualMovement(entity);
                    }
                }
            }
        });

        //
        // Manual Right Turn
        //
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed D"), "start turning right");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed RIGHT"), "start turning right");
        canvas.getActionMap().put("start turning right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                worldPanel.setManualMovementState("d", true);
                OdorWorldEntity entity = worldPanel.getFirstSelectedRotatingEntity();
                if (entity != null) {
                    entity.setManualMode(true);
                    entity.turnRight();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released D"), "stopTurningRight");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released RIGHT"), "stopTurningRight");
        canvas.getActionMap().put("stopTurningRight", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                worldPanel.setManualMovementState("d", false);
                OdorWorldEntity entity = worldPanel.getFirstSelectedRotatingEntity();
                if (entity != null) {
                    // case where a and d are both being pressed
                    if (worldPanel.getManualMovementState("a")) {
                        entity.turnLeft();
                    } else {
                        entity.stopTurning();
                        worldPanel.releaseManualMovement(entity);
                    }
                }
            }
        });

        // Debug Piccolo
        canvas.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK), "debug");
        canvas.getActionMap().put("debug", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                worldPanel.showPNodeDebugger();
            }
        });

    }
}
