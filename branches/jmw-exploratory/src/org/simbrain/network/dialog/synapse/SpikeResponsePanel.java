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
package org.simbrain.network.dialog.synapse;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.simbrain.network.NetworkUtils;
import org.simbrain.util.LabelledItemPanel;
import org.simnet.interfaces.SpikeResponder;
import org.simnet.interfaces.Synapse;
import org.simnet.synapses.spikeresponders.JumpAndDecay;
import org.simnet.synapses.spikeresponders.RiseAndDecay;
import org.simnet.synapses.spikeresponders.Step;

//Note: diff changeset 1490 and 1491 to see how to put general spike response features into this panel

/**
 * <b>SpikeResponsePanel</b>.  
 */
public class SpikeResponsePanel extends JPanel implements ActionListener {

    /** Null string. */
    public static final String NULL_STRING = "...";

    /** Main panel. */
    private JPanel mainPanel = new JPanel();

    /** Top panel. */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /** Spike response type. */
    private JComboBox cbSpikeResponseType = new JComboBox(SpikeResponder.getTypeList());

    /** Spike function panel. */
    private AbstractSpikeResponsePanel spikeFunctionPanel;

    /** Spike responder list. */
    private ArrayList spikeResponderList;

    /** Synapse list. */
    private ArrayList synapseList;

    /** Parent dialog. */
    private JDialog parentDialog;

    /** Have spike responders changed. */
    private boolean spikeRespondersHaveChanged = false;

    /**
     * This method is the default constructor.
     *
     * @param synapses List of synapses
     * @param parent Dialog calling this panel
     */
    public SpikeResponsePanel(final ArrayList synapses, final JDialog parent) {
        this.setLayout(new BorderLayout());
        synapseList = synapses;
        parentDialog = parent;
        spikeResponderList = getSpikeResponders();
        initSpikeResponseType();
        fillFieldValues();
        cbSpikeResponseType.addActionListener(this);

        topPanel.addItem("Spike response function", cbSpikeResponseType);
        this.add(BorderLayout.NORTH, topPanel);
        mainPanel.add(spikeFunctionPanel);
        this.add(BorderLayout.CENTER, mainPanel);
    }

    /**
     * @return The spike responder list.
     */
    private ArrayList getSpikeResponders() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < synapseList.size(); i++) {
            ret.add(((Synapse) synapseList.get(i)).getSpikeResponder());
        }

        return ret;
    }

    /**
     * Initalizes spike response type.
     */
    private void initSpikeResponseType() {
        SpikeResponder spikeResponder = (SpikeResponder) spikeResponderList.get(0);

        if (!NetworkUtils.isConsistent(spikeResponderList, SpikeResponder.class, "getType")) {
            cbSpikeResponseType.addItem(NULL_STRING);
            cbSpikeResponseType.setSelectedIndex(SpikeResponder.getTypeList().length);
            spikeFunctionPanel = new BlankSpikerPanel(); // Simply to serve as an empty panel
        } else if (spikeResponder instanceof Step) {
            cbSpikeResponseType.setSelectedIndex(SpikeResponder.getSpikerTypeIndex(Step.getName()));
            spikeFunctionPanel = new StepSpikerPanel();
            spikeFunctionPanel.setSpikeResponderList(spikeResponderList);
            spikeFunctionPanel.fillFieldValues();
        } else if (spikeResponder instanceof JumpAndDecay) {
            cbSpikeResponseType.setSelectedIndex(SpikeResponder.getSpikerTypeIndex(JumpAndDecay.getName()));
            spikeFunctionPanel = new JumpAndDecayPanel();
            spikeFunctionPanel.setSpikeResponderList(spikeResponderList);
            spikeFunctionPanel.fillFieldValues();
        } else if (spikeResponder instanceof RiseAndDecay) {
            cbSpikeResponseType.setSelectedIndex(SpikeResponder.getSpikerTypeIndex(RiseAndDecay.getName()));
            spikeFunctionPanel = new RiseAndDecayPanel(spikeResponder.getParent().getSource().getParentNetwork());
            spikeFunctionPanel.setSpikeResponderList(spikeResponderList);
            spikeFunctionPanel.fillFieldValues();
        }
    }

    /**
     * Change spike responders.
     */
    private void changeSpikeResponders() {
        if (cbSpikeResponseType.getSelectedItem().toString().equalsIgnoreCase(Step.getName())) {
            for (int i = 0; i < spikeResponderList.size(); i++) {
                ((Synapse) synapseList.get(i)).setSpikeResponder(new Step());
            }
        } else if (cbSpikeResponseType.getSelectedItem().toString().equalsIgnoreCase(JumpAndDecay.getName())) {
            for (int i = 0; i < spikeResponderList.size(); i++) {
                ((Synapse) synapseList.get(i)).setSpikeResponder(new JumpAndDecay());
            }
        } else if (cbSpikeResponseType.getSelectedItem().toString().equalsIgnoreCase(RiseAndDecay.getName())) {
            for (int i = 0; i < spikeResponderList.size(); i++) {
                ((Synapse) synapseList.get(i)).setSpikeResponder(new RiseAndDecay());
            }
        }
    }

    /** @see ActionListener */
    public void actionPerformed(final ActionEvent e) {
        spikeRespondersHaveChanged = true;

        SpikeResponder spikeResponder = (SpikeResponder) spikeResponderList.get(0);

        if (cbSpikeResponseType.getSelectedItem().equals(Step.getName())) {
            mainPanel.remove(spikeFunctionPanel);
            spikeFunctionPanel = new StepSpikerPanel();
            spikeFunctionPanel.fillDefaultValues();
            mainPanel.add(spikeFunctionPanel);
        } else if (cbSpikeResponseType.getSelectedItem().equals(JumpAndDecay.getName())) {
            mainPanel.remove(spikeFunctionPanel);
            spikeFunctionPanel = new JumpAndDecayPanel();
            spikeFunctionPanel.fillDefaultValues();
            mainPanel.add(spikeFunctionPanel);
        } else if (cbSpikeResponseType.getSelectedItem().equals(RiseAndDecay.getName())) {
            mainPanel.remove(spikeFunctionPanel);
            spikeFunctionPanel = new RiseAndDecayPanel(spikeResponder.getParent().getSource().getParentNetwork());
            spikeFunctionPanel.fillDefaultValues();
            mainPanel.add(spikeFunctionPanel);
        }

        parentDialog.pack();
        parentDialog.repaint();

    }

    /**
     * Populates the fields with current values.
     */
    public void fillFieldValues() {
        SpikeResponder spikeResponder = (SpikeResponder) spikeResponderList.get(0);

        spikeFunctionPanel.fillFieldValues();

    }

    /**
     * Called exterally to commit changes when dialog is closed.
     *
     */
    public void commitChanges() {
        for (int i = 0; i < spikeResponderList.size(); i++) {
            SpikeResponder spikeResponder = (SpikeResponder) spikeResponderList.get(0);
        }

        if (spikeRespondersHaveChanged) {
            changeSpikeResponders();
        }

        spikeFunctionPanel.setSpikeResponderList(getSpikeResponders());
        spikeFunctionPanel.commitChanges();
    }

    /**
     * @return Returns the spikerList.
     */
    public ArrayList getSpikeResponderList() {
        return spikeResponderList;
    }

    /**
     * @param spikerList The spikerList to set.
     */
    public void setSpikeResponderList(final ArrayList spikerList) {
        this.spikeResponderList = spikerList;
    }

    /**
     * Returns the respones function in a string.
     * @return responseFunction
     */
    public String getResponseFunction() {
        String responseFunction = cbSpikeResponseType.getSelectedItem().toString();
        return responseFunction;
    }

}
