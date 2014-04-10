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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.simbrain.network.groups.Group;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.group.GroupPropertiesPanel;
import org.simbrain.network.subnetworks.Hopfield;
import org.simbrain.network.subnetworks.Hopfield.HopfieldUpdate;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.ShowHelpAction;

/**
 * <b>DiscreteHopfieldPropertiesDialog</b> is a dialog box for setting the
 * properties of a discrete Hopfield network. Can be used in creation or
 * editing.
 */
public class HopfieldPropertiesPanel extends JPanel implements
GroupPropertiesPanel {

    /** Default number of neurons. */
    private static final int DEFAULT_NUM_NEURONS = 9;

    /** Parent Network Panel. */
    private NetworkPanel networkPanel;

    /** Number of neurons field. */
    private JTextField tfNumNeurons = new JTextField();

    /** Network type combo box. */
    private JComboBox<String> cbUpdateOrder = new JComboBox<String>(
            HopfieldUpdate.getUpdateFuncNames());

    private JCheckBox priorityChkBx = new JCheckBox();

    private JCheckBox shuffleUpdateOrder = new JCheckBox();

    {
        shuffleUpdateOrder.setToolTipText("Randomizes the order of the neuron" +
                " updates: \nThis random seqence is the same for \neach" +
                " update.");
    }

    /** The model subnetwork. */
    private Hopfield hopfield;

    /** Help Button. */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. */
    private ShowHelpAction helpAction;

    /** If true this is a creation panel. Otherwise it is an edit panel. */
    private boolean isCreationPanel;

    /**
     * Constructor for creating Hopfield networks.
     *
     * @param np parent network panel
     */
    public HopfieldPropertiesPanel(final NetworkPanel np) {
        this.networkPanel = np;
        isCreationPanel = true;
        initPanel();
    }

    /**
     * Constructor for editing.
     *
     * @param np parent network panel
     * @param hop network being modified.
     */
    public HopfieldPropertiesPanel(final NetworkPanel np, final Hopfield hop) {
        this.networkPanel = np;
        this.hopfield = hop;
        isCreationPanel = false;
        initPanel();
    }

    /**
     * Initialize the panel.
     */
    private void initPanel() {
        repaintPanel();
        fillFieldValues();
        addListeners();
    }

    /**
     * @return the update order.
     */
    public HopfieldUpdate getUpdateType() {
        return HopfieldUpdate.getUpdateFuncFromName((String)
                cbUpdateOrder.getSelectedItem());
    }

    private void repaintPanel() {
        removeAll();
        GridLayout lay = new GridLayout(0, 2);
        lay.setHgap(5);
        lay.setVgap(15);
        this.setLayout(lay);
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        layoutPanel();
        repaint();
        revalidate();
    }
    
    private synchronized void layoutPanel() {
        if (isCreationPanel) {
            add(new JLabel("Number of Neurons"));
            add(tfNumNeurons);
        }
        add(new JLabel("Update Order"));
        add(cbUpdateOrder);
        switch (getUpdateType()) {
            case SEQ : 
                add(new JLabel("By Priority"));
                add(priorityChkBx);
                add(new JLabel("Shuffle Order"));
                add(shuffleUpdateOrder);
                break;
            case SYNC :
                // No extra items needed
                break;
            case RAND :
                // No extra items needed
                break;
            default :
                throw new IllegalArgumentException("No such update function.");
        }
    }

    private void addListeners() {
        cbUpdateOrder.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                repaintPanel();
            }
        });
        priorityChkBx.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                shuffleUpdateOrder.setEnabled(!priorityChkBx.isSelected());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillFieldValues() {
        if (isCreationPanel) {
            hopfield = new Hopfield(null, DEFAULT_NUM_NEURONS);
            tfNumNeurons.setText("" + DEFAULT_NUM_NEURONS);
        }
        cbUpdateOrder.setSelectedItem(hopfield.getUpdateFunc().getName());
        priorityChkBx.setSelected(hopfield.isByPriority());
        shuffleUpdateOrder.setEnabled(!hopfield.isByPriority());
        shuffleUpdateOrder.setSelected(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean commitChanges() {
        try {
            if (isCreationPanel) {
                hopfield = new Hopfield(networkPanel.getNetwork(),
                        Integer.parseInt(tfNumNeurons.getText()));
            }
            hopfield.setUpdateFunc(getUpdateType());
            if (getUpdateType().equals(HopfieldUpdate.SEQ)) {
                hopfield.setByPriority(priorityChkBx.isSelected());
                if (!priorityChkBx.isSelected()) {
                    hopfield.randomizeSequence();
                }
            }
        } catch (NumberFormatException nfe) {
            return false; // Failure
        }
        return true; // Success
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHelpPath() {
        return "Pages/Network/network/hopfieldnetwork.html";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Group getGroup() {
        return hopfield;
    }

}
