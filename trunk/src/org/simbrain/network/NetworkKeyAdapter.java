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

import org.simbrain.network.actions.ConnectNeuronsAction;
import org.simbrain.network.nodes.NeuronNode;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;

import com.sun.corba.se.spi.orbutil.fsm.Action;

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
            ConnectNeuronsAction action = new ConnectNeuronsAction(
                    networkPanel, networkPanel.getSourceNeurons(), networkPanel
                            .getSelectedNeurons());
            action.actionPerformed(null);
            break;

        case KeyEvent.VK_3:
            // TODO: Put this in a list-of-neurons subclass of arraylist?
            // TODO: Explain in javadocs that selection classes take pnodes.
            // This below must be inefficient...
            {
                ArrayList list = (ArrayList) networkPanel.getSelectedModelNeurons();
                ArrayList sourceWeights = new ArrayList();
                for (Iterator i = list.iterator(); i.hasNext();) {
                    Neuron neuron = (Neuron) i.next();
                    for (Iterator j = neuron.getFanIn().iterator(); j.hasNext();) {
                        Synapse synapse = (Synapse) j.next();
                        sourceWeights.add(networkPanel.findSynapseNode(synapse));
                    }
                }
                networkPanel.clearSelection();
                networkPanel.setSelection(sourceWeights);
            }
            break;

        case KeyEvent.VK_4:
            {
                // This below must be inefficient...
                ArrayList list = (ArrayList) networkPanel.getSelectedModelNeurons();
                ArrayList sourceWeights = new ArrayList();
                for (Iterator i = list.iterator(); i.hasNext();) {
                    Neuron neuron = (Neuron) i.next();
                    for (Iterator j = neuron.getFanOut().iterator(); j.hasNext();) {
                        Synapse synapse = (Synapse) j.next();
                        sourceWeights.add(networkPanel.findSynapseNode(synapse));
                    }
                }
                networkPanel.clearSelection();
                networkPanel.setSelection(sourceWeights);
            }
            break;
        case KeyEvent.VK_5:
            {
                networkPanel.turnOffSynapseNodes();              
            }
            break;
        case KeyEvent.VK_6:
            {
                networkPanel.turnOnSynapseNodes();              
            }
            break;
        default:
            break;
        }
    }

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
