/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.LayoutDialog;
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * A dialog for adding multiple neurons to the network. User can specify a
 * neuron type and a layout.
 *
 * @author ztosi
 * @author jyoshimi
 */
public class AddNeuronsDialog extends StandardDialog {

    /** Default. */
    private static final long serialVersionUID = 1L;

    /** The default layout. */
    private static final Layout DEFAULT_LAYOUT = new GridLayout();

    /** The default neuron. */
    private static final NeuronUpdateRule DEFAULT_NEURON = new LinearNeuron();

    /** The layout to be used on the neurons. */
    private Layout layout = DEFAULT_LAYOUT;

    /** Default number of neurons. */
    private static final int DEFAULT_NUM_NEURONS = 25;

    /** The network panel neurons will be added to. */
    private final NetworkPanel networkPanel;

    /** The base neuron to copy. */
    private Neuron baseNeuron;

    /** Item panel where options will be displayed. */
    private LabelledItemPanel addNeuronsPanel = new LabelledItemPanel();

    /** Button allowing selection of type of neuron to add. **/
    private JButton selectNeuronType = new JButton();

    /** Button allowing selection of Layout. */
    private JButton selectLayout = new JButton();

    /** Text field where desired number of neurons is entered. */
    private JTextField numNeurons = new JTextField("" + DEFAULT_NUM_NEURONS);

    /** An ArrayList containing the GUI neurons. */
    private final ArrayList<NeuronNode> nodes = new ArrayList<NeuronNode>();

    /**
     * Constructs the dialog.
     *
     * @param networkPanel the panel the neurons are being added to.
     */
    public AddNeuronsDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        init();
    }

    /**
     * Initializes the add neurons panel with default settings.
     */
    private void init() {
        setTitle("Add Neurons...");

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        addNeuronsPanel.setMyNextItemRow(1);
        c.gridx = 0;
        c.gridy = addNeuronsPanel.getMyNextItemRow();

        networkPanel.clearSelection();

        baseNeuron = new Neuron(networkPanel.getRootNetwork(), DEFAULT_NEURON);
        setActionListeners();

        selectNeuronType.setText(baseNeuron.getUpdateRule().getDescription());
        selectLayout.setText(layout.getLayoutName());
        addNeuronsPanel.addItem("Number of Neurons: ", numNeurons);
        addNeuronsPanel.addItem("Select Neuron Type: ", selectNeuronType);
        addNeuronsPanel.addItem("Select Layout: ", selectLayout);
        numNeurons.setVisible(true);
        setContentPane(addNeuronsPanel);
    }

    /**
     * Set buttons' action listeners.
     */
    private void setActionListeners() {
        selectNeuronType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                ArrayList<NeuronNode> list = new ArrayList<NeuronNode>();
                list.add(new NeuronNode(networkPanel, baseNeuron));
                NeuronDialog dialog = new NeuronDialog(list);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                selectNeuronType.setText(baseNeuron.getUpdateRule()
                        .getDescription());
                AddNeuronsDialog.this.pack();
            }
        });

        selectLayout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                LayoutDialog lDialog = new LayoutDialog(LayoutDialog
                        .getCurrentLayout(), networkPanel);
                lDialog.pack();
                lDialog.setLocationRelativeTo(null);
                lDialog.setVisible(true);
                layout = LayoutDialog.getCurrentLayout();
                selectLayout.setText(layout.getLayoutName());
                AddNeuronsDialog.this.pack();
            }
        });

    }

    /**
     * Adds the neurons to the panel.
     */
    private void addNeuronsToPanel() {
        int number = Integer.parseInt(numNeurons.getText());
        for (int i = 0; i < number; i++) {
            Neuron neuron = new Neuron(networkPanel.getRootNetwork(),
                    baseNeuron);
            nodes.add(new NeuronNode(networkPanel, neuron));
            networkPanel.getRootNetwork().addNeuron(neuron);
        }
        networkPanel.setSelection(nodes);
        layout.setInitialLocation(networkPanel.getLastClickedPosition());
        layout.layoutNeurons(networkPanel.getSelectedModelNeurons());
        networkPanel.repaint();
    }

    /**
     * {@inheritDoc}
     */
    protected void closeDialogOk() {
        super.closeDialogOk();
        addNeuronsToPanel();
        dispose();
    }

    /**
     * {@inheritDoc}
     */
    protected void closeDialogCancel() {
        super.closeDialogCancel();
        dispose();
    }

}
