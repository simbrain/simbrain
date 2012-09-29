/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.QuickConnectPreferences;

/**
 * Add key bindings to network panel. Controls many keyboard shortcuts. Bindings
 * not found here are in the action classes.
 *
 * TODO: - Migrate some of the local actions here to "official" actions in the
 * action package
 */
public class KeyBindings {

    /**
     * Add key bindings.
     *
     * @param panel panel in which to add bindings.
     */
    public static void addBindings(final NetworkPanel panel) {

        // TODO: Change below if not all of this status (when in focused window)
        InputMap inputMap = panel
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Adjust activations
        inputMap.put(KeyStroke.getKeyStroke("UP"), "increment");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "increment");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "decrement");
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "decrement");
        panel.getActionMap().put("increment", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                panel.incrementSelectedObjects();
            }
        });
        panel.getActionMap().put("decrement", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                panel.decrementSelectedObjects();
            }
        });

        // Nudge objects
        inputMap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, ActionEvent.SHIFT_MASK),
                "up");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
                ActionEvent.SHIFT_MASK), "right");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,
                ActionEvent.SHIFT_MASK), "down");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
                ActionEvent.SHIFT_MASK), "left");
        panel.getActionMap().put("up", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                panel.nudge(0, -1);
            }
        });
        panel.getActionMap().put("right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                panel.nudge(1, 0);
            }
        });
        panel.getActionMap().put("down", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                panel.nudge(0, 1);
            }
        });
        panel.getActionMap().put("left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                panel.nudge(-1, 0);
            }
        });

//        // Undo
//        inputMap.put(
//            KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit
//                            .getDefaultToolkit().getMenuShortcutKeyMask()), "undo");
//        panel.getActionMap().put("undo", new AbstractAction() {
//            public void actionPerformed(ActionEvent e) {
//                panel.getUndoManager().undo();
//            }
//        });
//        // Redo
//        inputMap.put(
//                KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit
//                        .getDefaultToolkit().getMenuShortcutKeyMask() + 
//                        ActionEvent.SHIFT_MASK), "redo");
//        panel.getActionMap().put("redo", new AbstractAction() {
//            public void actionPerformed(ActionEvent e) {
//                panel.getUndoManager().redo();
//                panel.repaint();
//            }
//        });


        // Print debug info
        inputMap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.SHIFT_MASK),
                "debug");
        panel.getActionMap().put("debug", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.out.println(panel.getNetwork().toString());
            }
        });


        // Selection Mode
        inputMap.put(KeyStroke.getKeyStroke("S"), "selectionMode");
        panel.getActionMap().put("selectionMode",
                panel.getActionManager().getSelectionEditModeAction());

        // Pan Mode
        inputMap.put(KeyStroke.getKeyStroke("K"), "panMode");
        panel.getActionMap().put("panMode",
                panel.getActionManager().getPanEditModeAction());

        // Text Mode
        inputMap.put(KeyStroke.getKeyStroke("T"), "textMode");
        panel.getActionMap().put("textMode",
                panel.getActionManager().getTextEditModeAction());

        // Zoom mode
        inputMap.put(KeyStroke.getKeyStroke("Z"), "altPress");
        panel.getActionMap().put("altPress", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (panel.getEditMode().isZoomIn()) {
                    panel.setEditMode(EditMode.ZOOM_OUT);
                } else if (panel.getEditMode().isZoomOut()) {
                    panel.setEditMode(EditMode.ZOOM_IN);
                } else {
                    panel.setEditMode(EditMode.ZOOM_IN);
                }
            }
        });

        // Number keys
        inputMap.put(KeyStroke.getKeyStroke("1"), "setSource");
        panel.getActionMap().put("setSource", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                panel.setSourceNeurons();
                // TODO: This does not work when I use the action manager's
                // action. Not sure why not.
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("2"), "connectNeurons");
        panel.getActionMap().put("connectNeurons", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ConnectNeurons connection = QuickConnectPreferences
                        .getCurrentConnection();
                connection.connectNeurons(panel.getNetwork(),
                        panel.getSourceModelNeurons(),
                        panel.getSelectedModelNeurons());
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("3"), "selectIncoming");
        panel.getActionMap().put("selectIncoming",
                panel.getActionManager().getSelectIncomingWeightsAction());

        inputMap.put(KeyStroke.getKeyStroke("4"), "selectOutgoing");
        panel.getActionMap().put("selectOutgoing",
                panel.getActionManager().getSelectOutgoingWeightsAction());

        inputMap.put(KeyStroke.getKeyStroke("5"), "showSynapses");
        panel.getActionMap().put("showSynapses", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (panel.getWeightsVisible()) {
                    panel.setWeightsVisible(false);
                } else {
                    panel.setWeightsVisible(true);
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("6"), "guiOn");
        panel.getActionMap().put("guiOn", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (panel.isGuiOn()) {
                    panel.setGuiOn(false);
                } else {
                    panel.setGuiOn(true);
                }
            }
        });

    }

}
