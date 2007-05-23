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
package org.simbrain.network.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;

/**
 * Select incoming weights action weights action.
 */
public final class SelectIncomingWeightsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create select incoming weights action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public SelectIncomingWeightsAction(final NetworkPanel networkPanel) {

        super("Select Incoming Weights");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

//        Toolkit toolkit = Toolkit.getDefaultToolkit();
//        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_3, toolkit.getMenuShortcutKeyMask());
//
//        putValue(ACCELERATOR_KEY, keyStroke);
        putValue(SHORT_DESCRIPTION, "Select All Incoming Weights");
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
//      TODO: Put this in a list-of-neurons subclass of arraylist?
//      TODO: Explain in javadocs that selection classes take pnodes.
//      This below must be inefficient...
        ArrayList list = (ArrayList) networkPanel.getSelectedModelNeurons();
        ArrayList sourceWeights = new ArrayList();
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            Neuron neuron = (Neuron) i.next();
            for (Synapse synapse : neuron.getFanIn()) {
                sourceWeights.add(networkPanel.findSynapseNode(synapse));
            }
        }
        networkPanel.clearSelection();
        networkPanel.setSelection(sourceWeights);
    }
}