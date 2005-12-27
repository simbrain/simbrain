/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.nodes.SynapseNode;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.SpikingNeuron;
import org.simnet.interfaces.Synapse;
import org.simnet.synapses.ClampedSynapse;
import org.simnet.synapses.DeltaRuleSynapse;
import org.simnet.synapses.Hebbian;
import org.simnet.synapses.HebbianThresholdSynapse;
import org.simnet.synapses.OjaSynapse;
import org.simnet.synapses.RandomSynapse;
import org.simnet.synapses.ShortTermPlasticitySynapse;
import org.simnet.synapses.SubtractiveNormalizationSynapse;


/**
 * <b>SynapseDialog</b>
 */
public class SynapseDialog extends StandardDialog implements ActionListener {
    public static final String NULL_STRING = "...";
    private Box mainPanel = Box.createVerticalBox();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private LabelledItemPanel main_tab = new LabelledItemPanel();
    private SpikeResponsePanel spikeResponsePanel = null;
    private LabelledItemPanel topPanel = new LabelledItemPanel();
    private AbstractSynapsePanel synapsePanel = new ClampedSynapsePanel();
    private JTextField tfStrength = new JTextField();
    private JTextField tfIncrement = new JTextField();
    private JTextField tfUpBound = new JTextField();
    private JTextField tfLowBound = new JTextField();
    private JTextField tfDelay = new JTextField();
    private JLabel upperLabel = new JLabel("Upper bound");
    private JLabel lowerLabel = new JLabel("Lower bound");
    private JComboBox cbSynapseType = new JComboBox(Synapse.getTypeList());
    private ArrayList synapse_list = new ArrayList(); // The synapses being modified
    private ArrayList selection_list; // The pnodes which refer to them
    private boolean weightsHaveChanged = false;

    /**
     * This method is the default constructor.
     */
    public SynapseDialog(final ArrayList selectedSynapses) {
        selection_list = selectedSynapses;
        setSynapseList();
        init();
    }

    /**
     * Get the logical weights from the pnodeNeurons
     */
    public void setSynapseList() {
        synapse_list.clear();

        Iterator i = selection_list.iterator();

        while (i.hasNext()) {
            SynapseNode w = (SynapseNode) i.next();
            synapse_list.add(w.getSynapse());
        }
    }

    /** @see StandardDialog */
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }
    
    /**
     * Initialises the components on the panel.
     */
    private void init() {
        setTitle("Synapse Dialog");
        this.setLocation(500, 0); //Sets location of network dialog

        initSynapseType();
        synapsePanel.setSynapse_list(synapse_list);
        fillFieldValues();

        cbSynapseType.addActionListener(this);
        topPanel.addItem("Strength", tfStrength);
        topPanel.addItem("Increment", tfIncrement);

        String toolTipText = "<html>If text is grayed out, this field is only used for graphics purposes <p> (to determine what size this synapse should be).</html>";
        upperLabel.setToolTipText(toolTipText);
        lowerLabel.setToolTipText(toolTipText);
        topPanel.addItemLabel(upperLabel, tfUpBound);
        topPanel.addItemLabel(lowerLabel, tfLowBound);
        topPanel.addItem("Delay", tfDelay);
        topPanel.addItem("Synapse type", cbSynapseType);

        mainPanel.add(topPanel);
        mainPanel.add(synapsePanel);

        ArrayList spikeResponders = getSpikeResponders();

        if (spikeResponders.size() > 0) {
            spikeResponsePanel = new SpikeResponsePanel(spikeResponders, this);
            tabbedPane.addTab("Synaptic Efficacy", mainPanel);
            tabbedPane.addTab("Spike Response", spikeResponsePanel);
            setContentPane(tabbedPane);
        } else {
            setContentPane(mainPanel);
        }
    }

    /**
     * Returns true if any of the selected neurons are spiking neurons
     */
    private ArrayList getSpikeResponders() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < synapse_list.size(); i++) {
            Neuron source = ((SynapseNode) selection_list.get(i)).getSynapse().getSource();

            if (source instanceof SpikingNeuron) {
                ret.add(((SynapseNode) selection_list.get(i)).getSynapse());
            }
        }

        return ret;
    }

    /**
     * Initialize the main synapse panel based on the type of the selected synapses
     */
    public void initSynapseType() {
        Synapse synapse_ref = (Synapse) synapse_list.get(0);

        if (!NetworkUtils.isConsistent(synapse_list, Synapse.class, "getType")) {
            cbSynapseType.addItem(AbstractSynapsePanel.NULL_STRING);
            cbSynapseType.setSelectedIndex(Synapse.getTypeList().length);
            synapsePanel = new ClampedSynapsePanel();
            synapsePanel.setSynapse_list(synapse_list);
            synapsePanel.fillFieldValues();
        } else if (synapse_ref instanceof Hebbian) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(Hebbian.getName()));
            synapsePanel = new HebbianSynapsePanel();
            synapsePanel.setSynapse_list(synapse_list);
            synapsePanel.fillFieldValues();
        } else if (synapse_ref instanceof OjaSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(OjaSynapse.getName()));
            synapsePanel = new OjaSynapsePanel();
            synapsePanel.setSynapse_list(synapse_list);
            synapsePanel.fillFieldValues();
        } else if (synapse_ref instanceof RandomSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(RandomSynapse.getName()));
            synapsePanel = new RandomSynapsePanel();
            synapsePanel.setSynapse_list(synapse_list);
            synapsePanel.fillFieldValues();
        } else if (synapse_ref instanceof SubtractiveNormalizationSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(SubtractiveNormalizationSynapse.getName()));
            synapsePanel = new SubtractiveNormalizationSynapsePanel();
            synapsePanel.setSynapse_list(synapse_list);
            synapsePanel.fillFieldValues();
        } else if (synapse_ref instanceof ClampedSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(ClampedSynapse.getName()));
            synapsePanel = new ClampedSynapsePanel();
            synapsePanel.setSynapse_list(synapse_list);
            synapsePanel.fillFieldValues();
        } else if (synapse_ref instanceof ShortTermPlasticitySynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(ShortTermPlasticitySynapse.getName()));
            synapsePanel = new ShortTermPlasticitySynapsePanel();
            synapsePanel.setSynapse_list(synapse_list);
            synapsePanel.fillFieldValues();
        } else if (synapse_ref instanceof HebbianThresholdSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(HebbianThresholdSynapse.getName()));
            synapsePanel = new HebbianThresholdSynapsePanel();
            synapsePanel.setSynapse_list(synapse_list);
            synapsePanel.fillFieldValues();
        } else if (synapse_ref instanceof DeltaRuleSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(DeltaRuleSynapse.getName()));
            synapsePanel = new DeltaRuleSynapsePanel();
            synapsePanel.setSynapse_list(synapse_list);
            synapsePanel.fillFieldValues();
        }
    }

    /**
     * Change all the synapses from their current type  to the new selected type
     */
    public void changeSynapses() {
        if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(Hebbian.getName())) {
            for (int i = 0; i < synapse_list.size(); i++) {
                Synapse oldSynapse = (Synapse) synapse_list.get(i);
                Hebbian newSynapse = new Hebbian(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(OjaSynapse.getName())) {
            for (int i = 0; i < synapse_list.size(); i++) {
                Synapse oldSynapse = (Synapse) synapse_list.get(i);
                OjaSynapse newSynapse = new OjaSynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(RandomSynapse.getName())) {
            for (int i = 0; i < synapse_list.size(); i++) {
                Synapse oldSynapse = (Synapse) synapse_list.get(i);
                RandomSynapse newSynapse = new RandomSynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(SubtractiveNormalizationSynapse.getName())) {
            for (int i = 0; i < synapse_list.size(); i++) {
                Synapse oldSynapse = (Synapse) synapse_list.get(i);
                SubtractiveNormalizationSynapse newSynapse = new SubtractiveNormalizationSynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(ClampedSynapse.getName())) {
            for (int i = 0; i < synapse_list.size(); i++) {
                Synapse oldSynapse = (Synapse) synapse_list.get(i);
                ClampedSynapse newSynapse = new ClampedSynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(ShortTermPlasticitySynapse.getName())) {
            for (int i = 0; i < synapse_list.size(); i++) {
                Synapse oldSynapse = (Synapse) synapse_list.get(i);
                ShortTermPlasticitySynapse newSynapse = new ShortTermPlasticitySynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(HebbianThresholdSynapse.getName())) {
            for (int i = 0; i < synapse_list.size(); i++) {
                Synapse oldSynapse = (Synapse) synapse_list.get(i);
                ShortTermPlasticitySynapse newSynapse = new ShortTermPlasticitySynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(DeltaRuleSynapse.getName())) {
            for (int i = 0; i < synapse_list.size(); i++) {
                Synapse oldSynapse = (Synapse) synapse_list.get(i);
                DeltaRuleSynapse newSynapse = new DeltaRuleSynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        }
    }

    /**
     * Respond to synapse type changes.
     */
    public void actionPerformed(final ActionEvent e) {
        weightsHaveChanged = true;

        if (cbSynapseType.getSelectedItem().equals(Hebbian.getName())) {
            mainPanel.remove(synapsePanel);
            synapsePanel = new HebbianSynapsePanel();
            synapsePanel.fillDefaultValues();
            mainPanel.add(synapsePanel);
        } else if (cbSynapseType.getSelectedItem().equals(OjaSynapse.getName())) {
            mainPanel.remove(synapsePanel);
            synapsePanel = new OjaSynapsePanel();
            synapsePanel.fillDefaultValues();
            mainPanel.add(synapsePanel);
        } else if (cbSynapseType.getSelectedItem().equals(RandomSynapse.getName())) {
            mainPanel.remove(synapsePanel);
            synapsePanel = new RandomSynapsePanel();
            synapsePanel.fillDefaultValues();
            mainPanel.add(synapsePanel);
        } else if (cbSynapseType.getSelectedItem().equals(SubtractiveNormalizationSynapse.getName())) {
            mainPanel.remove(synapsePanel);
            synapsePanel = new SubtractiveNormalizationSynapsePanel();
            synapsePanel.fillDefaultValues();
            mainPanel.add(synapsePanel);
        } else if (cbSynapseType.getSelectedItem().equals(ClampedSynapse.getName())) {
            mainPanel.remove(synapsePanel);
            synapsePanel = new ClampedSynapsePanel();
            synapsePanel.fillDefaultValues();
            mainPanel.add(synapsePanel);
        } else if (cbSynapseType.getSelectedItem().equals(ShortTermPlasticitySynapse.getName())) {
            mainPanel.remove(synapsePanel);
            synapsePanel = new ShortTermPlasticitySynapsePanel();
            synapsePanel.fillDefaultValues();
            mainPanel.add(synapsePanel);
        } else if (cbSynapseType.getSelectedItem().equals(HebbianThresholdSynapse.getName())) {
            mainPanel.remove(synapsePanel);
            synapsePanel = new HebbianThresholdSynapsePanel();
            synapsePanel.fillDefaultValues();
            mainPanel.add(synapsePanel);
        } else if (cbSynapseType.getSelectedItem().equals(DeltaRuleSynapse.getName())) {
            mainPanel.remove(synapsePanel);
            synapsePanel = new DeltaRuleSynapsePanel();
            synapsePanel.fillDefaultValues();
            mainPanel.add(synapsePanel);
        }

        //Something different for mixed panel...
        pack();
    }

    /**
     * Set the initial values of dialog components
     */
    private void fillFieldValues() {
        Synapse synapse_ref = (Synapse) synapse_list.get(0);
        tfStrength.setText(Double.toString(synapse_ref.getStrength()));
        tfIncrement.setText(Double.toString(synapse_ref.getIncrement()));
        tfLowBound.setText(Double.toString(synapse_ref.getLowerBound()));
        tfUpBound.setText(Double.toString(synapse_ref.getUpperBound()));
        tfDelay.setText(Integer.toString(synapse_ref.getDelay()));

        synapsePanel.fillFieldValues();

        if (spikeResponsePanel != null) {
            spikeResponsePanel.fillFieldValues();
        }

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(synapse_list, Synapse.class, "getStrength")) {
            tfStrength.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapse_list, Synapse.class, "getIncrement")) {
            tfIncrement.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapse_list, Synapse.class, "getLowerBound")) {
            tfLowBound.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapse_list, Synapse.class, "getUpperBound")) {
            tfUpBound.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapse_list, Synapse.class, "getDelay")) {
            tfDelay.setText(NULL_STRING);
        }
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {
        for (int i = 0; i < synapse_list.size(); i++) {
            Synapse synapse_ref = (Synapse) synapse_list.get(i);

            if (tfStrength.getText().equals(NULL_STRING) == false) {
                synapse_ref.setStrength(Double.parseDouble(tfStrength.getText()));
            }

            if (tfIncrement.getText().equals(NULL_STRING) == false) {
                synapse_ref.setIncrement(Double.parseDouble(tfIncrement.getText()));
            }

            if (tfUpBound.getText().equals(NULL_STRING) == false) {
                synapse_ref.setUpperBound(Double.parseDouble(tfUpBound.getText()));
            }

            if (tfLowBound.getText().equals(NULL_STRING) == false) {
                synapse_ref.setLowerBound(Double.parseDouble(tfLowBound.getText()));
            }

            if (tfDelay.getText().equals(NULL_STRING) == false) {
                synapse_ref.setDelay(Integer.parseInt(tfDelay.getText()));
            }
        }

        if (weightsHaveChanged) {
            changeSynapses();
        }

        if (spikeResponsePanel != null) {
            spikeResponsePanel.commitChanges();
        }

        setSynapseList();
        synapsePanel.setSynapse_list(synapse_list);
        synapsePanel.commitChanges();
    }
}
