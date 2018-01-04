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
package org.simbrain.network.gui.dialogs.network;

import java.text.NumberFormat;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.MainLayoutPanel;
import org.simbrain.network.subnetworks.BoltzmannMachine;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * <b>BoltzmannMachineDialog</b> is used as an assistant to create Boltzmann networks.
 */
public class BoltzmannMachineDialog extends StandardDialog {

    /** Layout panel. */
    private MainLayoutPanel layoutPanel;

    /** Network Panel. */
    private NetworkPanel networkPanel;

    LabelledItemPanel propPanel = new LabelledItemPanel(); //TODOs
    int visibleNeurons;
    int hiddenNeurons;

    /**
     * This method is the default constructor.
     *
     * @param networkPanel Network panel
     */
    public BoltzmannMachineDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        layoutPanel = new MainLayoutPanel(false, this);
        init();
    }

    /**
     * Initializes all components used in dialog.
     */
    private void init() {

        setTitle("New Boltzmann Machine");

        JFormattedTextField tfVisibleNeurons = new JFormattedTextField(NumberFormat.getNumberInstance());
        tfVisibleNeurons.setValue(20);
        tfVisibleNeurons.addPropertyChangeListener("value", (evt) -> {
            visibleNeurons = ((Number) tfVisibleNeurons.getValue()).intValue();
            tfVisibleNeurons.setValue(visibleNeurons);
        });
        propPanel.addItem("Input neurons", tfVisibleNeurons);

        JFormattedTextField tfHiddenNeurons = new JFormattedTextField(NumberFormat.getNumberInstance());
        tfHiddenNeurons.setValue(30);
        tfHiddenNeurons.addPropertyChangeListener("value", (evt) -> {
            hiddenNeurons = ((Number) tfHiddenNeurons.getValue()).intValue();
            tfVisibleNeurons.setValue(hiddenNeurons);
        });
        propPanel.addItem("Hidden neurons", tfHiddenNeurons);

        visibleNeurons = ((Number) tfVisibleNeurons.getValue()).intValue();
        hiddenNeurons = ((Number) tfHiddenNeurons.getValue()).intValue();

        setContentPane(propPanel);

        // Help action 
        Action helpAction = new ShowHelpAction(""); //TODO
        addButton(new JButton(helpAction));

    }

    @Override
    protected void closeDialogOk() {
        BoltzmannMachine boltzmannMachine = new BoltzmannMachine(
                networkPanel.getNetwork(), visibleNeurons, hiddenNeurons,
                networkPanel.getLastClickedPosition());
        layoutPanel.commitChanges();

        networkPanel.getNetwork().addGroup(boltzmannMachine);
        networkPanel.syncToModel(); //TODO
        super.closeDialogOk();
    }

}
