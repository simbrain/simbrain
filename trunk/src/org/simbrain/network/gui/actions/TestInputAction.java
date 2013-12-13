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
package org.simbrain.network.gui.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPanel;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.NetworkSelectionEvent;
import org.simbrain.network.gui.NetworkSelectionListener;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Action to construct a test input panel. The user of this class provides the
 * input neurons and network panel from which the action gets the network to be
 * updated. If input neurons are not provided, selected neurons are used as
 * input neurons.
 *
 * @author Jeff Yoshimi
 * @author Lam Nguyen
 */
public class TestInputAction extends AbstractAction {

    {
        putValue(NAME, "Test network...");
        putValue(SHORT_DESCRIPTION, "Test network...");
        putValue(SMALL_ICON, ResourceManager.getImageIcon("TestInput.png"));
    }

    /** Network panel. */
    private NetworkPanel networkPanel;

    /** The panel used to test inputs to a network. */
    private JPanel testInputPanel;

    /** The nodes to test. */
    private List<Neuron> inputNeurons;

    /**
     * Construct action.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public TestInputAction(NetworkPanel networkPanel) {

        super("Test inputs...");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        // add a selection listener to update state based on selection
        networkPanel.addSelectionListener(new NetworkSelectionListener() {
            /** @see NetworkSelectionListener */
            public void selectionChanged(NetworkSelectionEvent event) {
                updateAction();
            }
        });
    }

    /**
     * Set test input panel based on number of selected neurons.
     */
    private void updateAction() {
        int numNeurons = networkPanel.getSelectedNeurons().size();

        if (numNeurons > 0) {
            inputNeurons = networkPanel.getSelectedModelNeurons();
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

    /**
     * Initialize and display the test input panel.
     */
    public void actionPerformed(ActionEvent event) {
        updateAction();
        testInputPanel = new TestInputPanel(networkPanel, inputNeurons);
        networkPanel.displayPanel(testInputPanel, "Test inputs");
    }
}