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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.simbrain.network.gui.actions.SelectIncomingWeightsAction;
import org.simbrain.network.gui.actions.SelectOutgoingWeightsAction;
import org.simbrain.network.gui.actions.connection.ConnectNeuronsAction;

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

        // Nudge things
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,
                ActionEvent.SHIFT_MASK), "up");
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

        // TODO: Figure this out.  Also need released.
        inputMap.put(KeyStroke.getKeyStroke("Alt"), "altPress");
        panel.getActionMap().put("altPress", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Here");
                if (panel.getEditMode().isZoomIn()) {
                    panel.setEditMode(EditMode.ZOOM_OUT);
                }
            }
        });

        // Number keys
        inputMap.put(KeyStroke.getKeyStroke("1"), "setSource");
        panel.getActionMap().put("setSource", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                panel.setSourceNeurons(); //TODO: I could not do this from action manager
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("2"), "connectNeurons");
        panel.getActionMap().put("connectNeurons", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                //TODO: It should be possible for this to be handled via action manager
                ConnectNeuronsAction connectAction = new ConnectNeuronsAction(
                        panel, panel.getSourceModelNeurons(), panel
                                .getSelectedModelNeurons());
                connectAction.actionPerformed(null);
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
