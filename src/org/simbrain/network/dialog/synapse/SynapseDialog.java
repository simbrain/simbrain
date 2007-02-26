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

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.actions.ShowHelpAction;
import org.simbrain.network.nodes.SynapseNode;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.SpikingNeuron;
import org.simnet.interfaces.Synapse;
import org.simnet.synapses.ClampedSynapse;
import org.simnet.synapses.Hebbian;
import org.simnet.synapses.HebbianCPCA;
import org.simnet.synapses.HebbianThresholdSynapse;
import org.simnet.synapses.OjaSynapse;
import org.simnet.synapses.RandomSynapse;
import org.simnet.synapses.ShortTermPlasticitySynapse;
import org.simnet.synapses.SignalSynapse;
import org.simnet.synapses.SubtractiveNormalizationSynapse;
import org.simnet.synapses.TraceSynapse;


/**
 * <b>SynapseDialog</b>.
 */
public class SynapseDialog extends StandardDialog implements ActionListener {

    /** Null string. */
    public static final String NULL_STRING = "...";

    /** Main panel. */
    private Box mainPanel = Box.createVerticalBox();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Spike response panel. */
    private SpikeResponsePanel spikeResponsePanel = null;

    /** Top panel. */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /** Synapse panel. */
    private AbstractSynapsePanel synapsePanel = new ClampedSynapsePanel();

    /** Strength field. */
    private JTextField tfStrength = new JTextField();

    /** Increment field. */
    private JTextField tfIncrement = new JTextField();

    /** Upper bound field. */
    private JTextField tfUpBound = new JTextField();

    /** Lower bound field. */
    private JTextField tfLowBound = new JTextField();

    /** Delay field. */
    private JTextField tfDelay = new JTextField();
    
    /** Change Weight field. */
    private JTextField tfCWeight = new JTextField();

    /** Upper label. */
    private JLabel upperLabel = new JLabel("Upper bound");

    /** Lower label. */
    private JLabel lowerLabel = new JLabel("Lower bound");

    /** Synapse type combo box. */
    private JComboBox cbSynapseType = new JComboBox(Synapse.getTypeList());

    /** The synapses being modified. */
    private ArrayList synapseList = new ArrayList();

    /** The pnodes which refer to them. */
    private ArrayList selectionList;

    /** Weights have changed boolean. */
    private boolean weightsHaveChanged = false;

    /** Help Button. */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. */
    private ShowHelpAction helpAction = new ShowHelpAction();


    /**
     * This method is the default constructor.
     * @param selectedSynapses LIst of synapses that are selected
     */
    public SynapseDialog(final ArrayList selectedSynapses) {
        selectionList = selectedSynapses;
        setSynapseList();
        init();
    }

    /**
     * Get the logical weights from the pnodeNeurons.
     */
    public void setSynapseList() {
        synapseList.clear();

        Iterator i = selectionList.iterator();

        while (i.hasNext()) {
            SynapseNode w = (SynapseNode) i.next();
            synapseList.add(w.getSynapse());
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

        initSynapseType();
        synapsePanel.setSynapseList(synapseList);
        fillFieldValues();
        updateHelp();

        helpButton.setAction(helpAction);
        this.addButton(helpButton);
        cbSynapseType.addActionListener(this);
        topPanel.addItem("Strength", tfStrength);
        topPanel.addItem("Increment", tfIncrement);

        String toolTipText = "<html>If text is grayed out, "
                + "this field is only used for graphics purposes <p> "
                + "(to determine what size this synapse should be).</html>";
        upperLabel.setToolTipText(toolTipText);
        lowerLabel.setToolTipText(toolTipText);
        topPanel.addItemLabel(upperLabel, tfUpBound);
        topPanel.addItemLabel(lowerLabel, tfLowBound);
        topPanel.addItem("Delay", tfDelay);
        topPanel.addItem("Synapse type", cbSynapseType);
        topPanel.addItem("Change of Weight", tfCWeight);

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
     * @return true if any of the selected neurons are spiking neurons.
     */
    private ArrayList getSpikeResponders() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < synapseList.size(); i++) {
            Neuron source = ((SynapseNode) selectionList.get(i)).getSynapse().getSource();

            if (source instanceof SpikingNeuron) {
                ret.add(((SynapseNode) selectionList.get(i)).getSynapse());
            }
        }

        return ret;
    }

    /**
     * Initialize the main synapse panel based on the type of the selected synapses.
     */
    public void initSynapseType() {
        Synapse synapseRef = (Synapse) synapseList.get(0);

        if (!NetworkUtils.isConsistent(synapseList, Synapse.class, "getType")) {
            cbSynapseType.addItem(AbstractSynapsePanel.NULL_STRING);
            cbSynapseType.setSelectedIndex(Synapse.getTypeList().length);
            synapsePanel = new ClampedSynapsePanel();
            synapsePanel.setSynapseList(synapseList);
            synapsePanel.fillFieldValues();
        } else if (synapseRef instanceof Hebbian) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(Hebbian.getName()));
            synapsePanel = new HebbianSynapsePanel();
            synapsePanel.setSynapseList(synapseList);
            synapsePanel.fillFieldValues();
        } else if (synapseRef instanceof OjaSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(OjaSynapse.getName()));
            synapsePanel = new OjaSynapsePanel();
            synapsePanel.setSynapseList(synapseList);
            synapsePanel.fillFieldValues();
        } else if (synapseRef instanceof RandomSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(RandomSynapse.getName()));
            synapsePanel = new RandomSynapsePanel();
            synapsePanel.setSynapseList(synapseList);
            synapsePanel.fillFieldValues();
        } else if (synapseRef instanceof SubtractiveNormalizationSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(SubtractiveNormalizationSynapse.getName()));
            synapsePanel = new SubtractiveNormalizationSynapsePanel();
            synapsePanel.setSynapseList(synapseList);
            synapsePanel.fillFieldValues();
        } else if (synapseRef instanceof ClampedSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(ClampedSynapse.getName()));
            synapsePanel = new ClampedSynapsePanel();
            synapsePanel.setSynapseList(synapseList);
            synapsePanel.fillFieldValues();
        } else if (synapseRef instanceof ShortTermPlasticitySynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(ShortTermPlasticitySynapse.getName()));
            synapsePanel = new ShortTermPlasticitySynapsePanel();
            synapsePanel.setSynapseList(synapseList);
            synapsePanel.fillFieldValues();
        } else if (synapseRef instanceof HebbianThresholdSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(HebbianThresholdSynapse.getName()));
            synapsePanel = new HebbianThresholdSynapsePanel();
            synapsePanel.setSynapseList(synapseList);
            synapsePanel.fillFieldValues();
        } else if (synapseRef instanceof SignalSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(SignalSynapse.getName()));
            synapsePanel = new SignalSynapsePanel();
            synapsePanel.setSynapseList(synapseList);
            synapsePanel.fillFieldValues();
        } else if (synapseRef instanceof TraceSynapse) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(TraceSynapse.getName()));
            synapsePanel = new TraceSynapsePanel();
            synapsePanel.setSynapseList(synapseList);
            synapsePanel.fillFieldValues();
        } else if (synapseRef instanceof HebbianCPCA) {
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(HebbianCPCA.getName()));
            synapsePanel = new HebbianCPCAPanel();
            synapsePanel.setSynapseList(synapseList);
            synapsePanel.fillFieldValues();
        }
    }

    /**
     * Change all the synapses from their current type  to the new selected type.
     */
    public void changeSynapses() {
        if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(Hebbian.getName())) {
            for (int i = 0; i < synapseList.size(); i++) {
                Synapse oldSynapse = (Synapse) synapseList.get(i);
                Hebbian newSynapse = new Hebbian(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(OjaSynapse.getName())) {
            for (int i = 0; i < synapseList.size(); i++) {
                Synapse oldSynapse = (Synapse) synapseList.get(i);
                OjaSynapse newSynapse = new OjaSynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(RandomSynapse.getName())) {
            for (int i = 0; i < synapseList.size(); i++) {
                Synapse oldSynapse = (Synapse) synapseList.get(i);
                RandomSynapse newSynapse = new RandomSynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(
                SubtractiveNormalizationSynapse.getName())) {
            for (int i = 0; i < synapseList.size(); i++) {
                Synapse oldSynapse = (Synapse) synapseList.get(i);
                SubtractiveNormalizationSynapse newSynapse = new SubtractiveNormalizationSynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(ClampedSynapse.getName())) {
            for (int i = 0; i < synapseList.size(); i++) {
                Synapse oldSynapse = (Synapse) synapseList.get(i);
                ClampedSynapse newSynapse = new ClampedSynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(ShortTermPlasticitySynapse.getName())) {
            for (int i = 0; i < synapseList.size(); i++) {
                Synapse oldSynapse = (Synapse) synapseList.get(i);
                ShortTermPlasticitySynapse newSynapse = new ShortTermPlasticitySynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(HebbianThresholdSynapse.getName())) {
            for (int i = 0; i < synapseList.size(); i++) {
                Synapse oldSynapse = (Synapse) synapseList.get(i);
                HebbianThresholdSynapse newSynapse = new HebbianThresholdSynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(SignalSynapse.getName())) {
            for (int i = 0; i < synapseList.size(); i++) {
                Synapse oldSynapse = (Synapse) synapseList.get(i);
                SignalSynapse newSynapse = new SignalSynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(TraceSynapse.getName())) {
            for (int i = 0; i < synapseList.size(); i++) {
                Synapse oldSynapse = (Synapse) synapseList.get(i);
                TraceSynapse newSynapse = new TraceSynapse(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        } else if (cbSynapseType.getSelectedItem().toString().equalsIgnoreCase(HebbianCPCA.getName())) {
            for (int i = 0; i < synapseList.size(); i++) {
                Synapse oldSynapse = (Synapse) synapseList.get(i);
                HebbianCPCA newSynapse = new HebbianCPCA(oldSynapse);
                oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
            }
        }
    }

    /**
     * Respond to synapse type changes.
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        weightsHaveChanged = true;

        updateHelp();

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
        } else if (cbSynapseType.getSelectedItem().equals(SignalSynapse.getName())) {
            mainPanel.remove(synapsePanel);
            synapsePanel = new SignalSynapsePanel();
            synapsePanel.fillDefaultValues();
            mainPanel.add(synapsePanel);
        } else if (cbSynapseType.getSelectedItem().equals(TraceSynapse.getName())) {
            mainPanel.remove(synapsePanel);
            synapsePanel = new TraceSynapsePanel();
            synapsePanel.fillDefaultValues();
            mainPanel.add(synapsePanel);
        } else if (cbSynapseType.getSelectedItem().equals(HebbianCPCA.getName())) {
            mainPanel.remove(synapsePanel);
            synapsePanel = new TraceSynapsePanel();
            synapsePanel.fillDefaultValues();
            mainPanel.add(synapsePanel);
        }

        //Something different for mixed panel...
        pack();
    }

    /**
     * Set the initial values of dialog components.
     */
    private void fillFieldValues() {
        Synapse synapseRef = (Synapse) synapseList.get(0);
        tfStrength.setText(Double.toString(synapseRef.getStrength()));
        tfIncrement.setText(Double.toString(synapseRef.getIncrement()));
        tfLowBound.setText(Double.toString(synapseRef.getLowerBound()));
        tfUpBound.setText(Double.toString(synapseRef.getUpperBound()));
        tfDelay.setText(Integer.toString(synapseRef.getDelay()));
		tfCWeight.setText(Double.toString(synapseRef.getChgWeight()));

        synapsePanel.fillFieldValues();

        if (spikeResponsePanel != null) {
            spikeResponsePanel.fillFieldValues();
        }

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(synapseList, Synapse.class, "getStrength")) {
            tfStrength.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapseList, Synapse.class, "getIncrement")) {
            tfIncrement.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapseList, Synapse.class, "getLowerBound")) {
            tfLowBound.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapseList, Synapse.class, "getUpperBound")) {
            tfUpBound.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(synapseList, Synapse.class, "getDelay")) {
            tfDelay.setText(NULL_STRING);
        }
        
        if (!NetworkUtils.isConsistent(synapseList, Synapse.class, "getChgWeight")) {
            tfCWeight.setText(NULL_STRING);
        }
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < synapseList.size(); i++) {
            Synapse synapseRef = (Synapse) synapseList.get(i);

            if (!tfStrength.getText().equals(NULL_STRING)) {
                synapseRef.setStrength(Double.parseDouble(tfStrength.getText()));
            }

            if (!tfIncrement.getText().equals(NULL_STRING)) {
                synapseRef.setIncrement(Double.parseDouble(tfIncrement.getText()));
            }

            if (!tfUpBound.getText().equals(NULL_STRING)) {
                synapseRef.setUpperBound(Double.parseDouble(tfUpBound.getText()));
            }

            if (!tfLowBound.getText().equals(NULL_STRING)) {
                synapseRef.setLowerBound(Double.parseDouble(tfLowBound.getText()));
            }

            if (!tfDelay.getText().equals(NULL_STRING)) {
                synapseRef.setDelay(Integer.parseInt(tfDelay.getText()));
            }
            
            if (!tfCWeight.getText().equals(NULL_STRING)) {
                synapseRef.setChgWeight(Double.parseDouble(tfCWeight.getText()));
            }
        }

        if (weightsHaveChanged) {
            changeSynapses();
        }

        if (spikeResponsePanel != null) {
            spikeResponsePanel.commitChanges();
        }

        ((Synapse) synapseList.get(0)).getSource().getParentNetwork().getRootNetwork().fireNetworkChanged();
        setSynapseList();
        synapsePanel.setSynapseList(synapseList);
        synapsePanel.commitChanges();
    }
    /**
      * Set the help page based on the currently selected neuron type.
      */
        private void updateHelp() {
            if (cbSynapseType.getSelectedItem() == NULL_STRING) {
                helpAction.setTheURL("Network/synapse.html");
            } else {
                String spacelessString = cbSynapseType.getSelectedItem().toString().replace(" ", "");
                helpAction.setTheURL("Network/synapse/" + spacelessString + ".html");
            }
        }
}