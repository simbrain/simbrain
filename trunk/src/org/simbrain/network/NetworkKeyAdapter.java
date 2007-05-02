/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;

import org.simbrain.network.actions.SelectIncomingWeightsAction;
import org.simbrain.network.actions.SelectOutgoingWeightsAction;
import org.simbrain.network.actions.connection.ConnectNeuronsAction;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.TextHandler;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;

/**
 * Network key adapter.
 */
class NetworkKeyAdapter extends KeyAdapter {

    /** Network panel. */
    private NetworkPanel networkPanel;

    /**
     * Network key adapter.
     *
     * @param networkPanel Network panel
     */
    public NetworkKeyAdapter(final NetworkPanel networkPanel) {

        this.networkPanel = networkPanel;
        handler = new TextHandler(networkPanel);

    }

    /**
     * Responds to key pressed events.
     *
     * @param e Key event
     */
    public void keyPressed(final KeyEvent e) {
        int keycode = e.getKeyCode();
        switch (keycode) {
        case KeyEvent.VK_LEFT:

            if (e.isShiftDown()) {
                networkPanel.nudge(-1, 0);
            } else {
                networkPanel.decrementSelectedObjects();
            }

            break;

        case KeyEvent.VK_RIGHT:

            if (e.isShiftDown()) {
                networkPanel.nudge(1, 0);
            } else {
                networkPanel.incrementSelectedObjects();
            }

            break;

        case KeyEvent.VK_UP:

            if (e.isShiftDown()) {
                networkPanel.nudge(0, -1);
            } else {
                networkPanel.incrementSelectedObjects();
            }

            break;

        case KeyEvent.VK_DOWN:

            if (e.isShiftDown()) {
                networkPanel.nudge(0, 1);
            } else {
                networkPanel.decrementSelectedObjects();
            }

            break;

        case KeyEvent.VK_U:

            networkPanel.clearSelection();

            break;

        case KeyEvent.VK_ALT:
            if (networkPanel.getEditMode().isZoomIn()) {
                networkPanel.setEditMode(EditMode.ZOOM_OUT);
            }
            break;

        case KeyEvent.VK_1:
            networkPanel.setSourceNeurons();
            break;

        case KeyEvent.VK_2:
            // If neurons have been selected, create an action which will
            // connect selected neurons to this one
            ConnectNeuronsAction connectAction = new ConnectNeuronsAction(
                    networkPanel, networkPanel.getSourceModelNeurons(), networkPanel.getSelectedModelNeurons());
            connectAction.actionPerformed(null);
            break;

        case KeyEvent.VK_3:
            SelectIncomingWeightsAction inWeightAction = new SelectIncomingWeightsAction(networkPanel);
            inWeightAction.actionPerformed(null);
            break;

        case KeyEvent.VK_4:
            SelectOutgoingWeightsAction outWeightAction = new SelectOutgoingWeightsAction(networkPanel);
            outWeightAction.actionPerformed(null);
            break;
        case KeyEvent.VK_5:
            {
                if (networkPanel.isSynapseNodesOn()){
                    networkPanel.setSynapseNodesOn(false);
                } else {
                    networkPanel.setSynapseNodesOn(true);
                }
            }
            break;
        case KeyEvent.VK_6:
            {
                if (networkPanel.isGuiOn()) {
                    networkPanel.setGuiOn(false);
                } else {
                    networkPanel.setGuiOn(true);
                }
            }
            break;
        case KeyEvent.VK_7:
        {
                networkPanel.addInputEventListener(handler);
        }
        break;
        case KeyEvent.VK_8:
        {
            for(NeuronNode node : networkPanel.getNeuronNodes()) {
                node.pushViewPositionToModel();
            }
        }
        break;
        default:
            break;
        }
    }
    TextHandler handler;

    /**
     * Responds to key released events.
     *
     * @param e Key event
     */
    public void keyReleased(final KeyEvent e) {
        if (networkPanel.getEditMode().isZoomOut()) {
            networkPanel.setEditMode(EditMode.ZOOM_IN);
        }

    }

}
