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
package org.simbrain.network.gui.dialogs.synapse;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.actions.ShowHelpAction;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.SpikingNeuronUpdateRule;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.synapses.ClampedSynapse;
import org.simbrain.network.synapses.Hebbian;
import org.simbrain.network.synapses.HebbianCPCA;
import org.simbrain.network.synapses.HebbianThresholdSynapse;
import org.simbrain.network.synapses.OjaSynapse;
import org.simbrain.network.synapses.RandomSynapse;
import org.simbrain.network.synapses.STDPSynapse;
import org.simbrain.network.synapses.ShortTermPlasticitySynapse;
import org.simbrain.network.synapses.SignalSynapse;
import org.simbrain.network.synapses.SimpleSynapse;
import org.simbrain.network.synapses.SubtractiveNormalizationSynapse;
import org.simbrain.network.synapses.TDSynapse;
import org.simbrain.network.synapses.TraceSynapse;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * The <b>SynapseDialog</b> is initialized with a list of synapses. When
 * the dialog is closed the synapses are changed based on the state of the
 * dialog.
 *
 * TODO: Rewrite using method (or a form of the method) in NeuronDialog.java
 */
public class SynapseDialog extends StandardDialog implements ActionListener {

    /** Null string. */
    public static final String NULL_STRING = "...";

    /** Tabbed pane (only used if a spike responder pane is needed) */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main panel. */
    private Box mainPanel = Box.createVerticalBox();

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

    /** Upper label. */
    private JLabel upperLabel = new JLabel("Upper bound");

    /** Lower label. */
    private JLabel lowerLabel = new JLabel("Lower bound");

    /** Synapse type combo box. */
    private JComboBox cbSynapseType = new JComboBox(Synapse.getTypeList());

    /** The synapses being modified. */
    private List<Synapse> synapseList = new ArrayList<Synapse>();

    /** Weights have changed boolean. */
    private boolean weightsHaveChanged = false;

    /** Help Button. */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. */
    private ShowHelpAction helpAction = new ShowHelpAction();

    /** Associates synapse creator utilities with synapses by name. */
    private Map<String, Creator> creatorsByName = new TreeMap<String, Creator>(
            String.CASE_INSENSITIVE_ORDER);

    /** Associates synapse creator utilities with synapses by class. */
    private Map<Class, Creator> creatorsByClass = new HashMap<Class, Creator>();

    /**
     * Constructor for a list of SynapseNodes.
     *
     * @param synapseList list of synapses to modify
     */
    public SynapseDialog(final List<Synapse> synapseList) {
        this.synapseList = (List<Synapse>) synapseList;
        initializeCreators();
        init();
    }
    
    /** @see StandardDialog */
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Initializes the components on the panel.
     */
    private void init() {
        setTitle("Synapse Dialog");

        initSynapseType();
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

        mainPanel.add(topPanel);
        mainPanel.add(synapsePanel);

        // Add tab for setting spike responders, if needed
        ArrayList<Synapse> spikeRespondingSynapses = getSpikeRespondingSynapses();
        if (spikeRespondingSynapses.size() > 0) {
            spikeResponsePanel = new SpikeResponsePanel(spikeRespondingSynapses, this);
            tabbedPane.addTab("Synaptic Efficacy", mainPanel);
            tabbedPane.addTab("Spike Response", spikeResponsePanel);
            setContentPane(tabbedPane);
            updateHelp();
        } else {
            setContentPane(mainPanel);
        }
    }

    /**
     * Retrieve those synapses which are spike responders.
     *
     * @return the list of synapses which are spike responders
     */
    private ArrayList<Synapse> getSpikeRespondingSynapses() {
        ArrayList<Synapse> ret = new ArrayList<Synapse>();

        for (Synapse synapse : synapseList) {
            Neuron source = synapse.getSource();
            if ((source.getUpdateRule() instanceof SpikingNeuronUpdateRule) && (source != null)) {
                ret.add(synapse);
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
            synapsePanel = new ClampedSynapsePanel(); // Default to clamped synapse panel
        } else {
            Creator creator = creatorsByClass.get(synapseRef.getClass());
            if (creator == null) {
                return;
            }
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(creator.getName()));
            synapsePanel = creator.createPanel();
        }
        synapsePanel.setSynapseList(synapseList);
        synapsePanel.fillFieldValues();
    }

    /**
     * Change all the synapses from their current type to the new specified in
     * the dialog.
     */
    public void changeSynapseTypes() {
        
        Creator creator = creatorsByName.get(cbSynapseType.getSelectedItem().toString());
        if (creator == null) {
            return;
        }

        //TODO: Only change those that have actually changed?
        for (int i = 0; i < synapseList.size(); i++) {
            Synapse oldSynapse = (Synapse) synapseList.get(i);
            Synapse newSynapse = creator.createSynapse(oldSynapse);
            synapseList.set(i, newSynapse);
            // Don't need this if the relevant synapses have not yet been added to a network
            //  (when the dialog is used to set an unattached collection of neurons)
            if (oldSynapse.getSource() != null) {
                    oldSynapse.getSource().getParentNetwork().changeSynapseType(oldSynapse, newSynapse);
            }
        }
    }

    /**
     * Respond to synapse type changes.
     *
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        weightsHaveChanged = true;

        updateHelp();

        Creator creator = creatorsByName.get(cbSynapseType.getSelectedItem());
        
        if (creator != null) {
            mainPanel.remove(synapsePanel);
            synapsePanel = creator.createPanel();
            synapsePanel.fillDefaultValues();
            mainPanel.add(synapsePanel);
        }
        
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

        synapsePanel.fillFieldValues();
        synapsePanel.setSynapseList(synapseList);

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
            
        }

        if (weightsHaveChanged) {
            changeSynapseTypes();
        }

        if (spikeResponsePanel != null) {
            spikeResponsePanel.commitChanges();
        }

        Synapse firstSynapse = synapseList.get(0);
        if (firstSynapse.getParentNetwork() != null) {
            firstSynapse.getParentNetwork().getRootNetwork().fireNetworkChanged();
        }
        synapsePanel.setSynapseList(synapseList);
        synapsePanel.commitChanges();
    }

     /**
      * Set the help page based on the currently selected synapse type.
      */
    public void updateHelp() {
        if (cbSynapseType.getSelectedItem() == NULL_STRING) {
            helpAction.setTheURL("Network/synapse.html");
        } else if (spikeResponsePanel != null) {
            String spacelessString = spikeResponsePanel.getResponseFunction().replace(" ", "");
            helpAction.setTheURL("Network/synapse/spikeresponders/" + spacelessString + ".html");
        } else {
            String spacelessString = cbSynapseType.getSelectedItem().toString().replace(" ", "");
            helpAction.setTheURL("Network/synapse/" + spacelessString + ".html");
        }
    }

    /**
     * Utility for producing information associated with synapses.
     */
    private static interface Creator {

        /**
         * Returns the name of the synapse.
         *
         * @return name of synapse.
         */
        String getName();

        /**
         * Creates a new synapse based on an old synapse. Used in changing
         * synapse type.
         * 
         * @param old the old synapse
         * @return the new synapse
         */
        Synapse createSynapse(Synapse old);
        
        /**
         * Returns the type of synapse panel associated with this synapse type.
         * 
         * @return the new synapse panel.
         */
        AbstractSynapsePanel createPanel();
    }
    
    /**
     * Creates instances of the Creator interface.  These are added to
     * two maps which are used to switch based on names or on the class
     * type
     */
    private void initializeCreators() {
        Creator creator;
        
        creator = new Creator() {
            public String getName() {
                return Hebbian.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new Hebbian(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new HebbianSynapsePanel();
            }
        };
        
        creatorsByName.put(Hebbian.getName(), creator);
        creatorsByClass.put(Hebbian.class, creator);
        
        creator = new Creator() {
            public String getName() {
                return OjaSynapse.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new OjaSynapse(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new OjaSynapsePanel();
            }
        };

        creatorsByName.put(OjaSynapse.getName(), creator);
        creatorsByClass.put(OjaSynapse.class, creator);
               
        creator = new Creator() {
            public String getName() {
                return RandomSynapse.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new RandomSynapse(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new RandomSynapsePanel();
            }
        };
        
        creatorsByName.put(RandomSynapse.getName(), creator);
        creatorsByClass.put(RandomSynapse.class, creator);

        creator = new Creator() {
            public String getName() {
                return SubtractiveNormalizationSynapse.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new SubtractiveNormalizationSynapse(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new SubtractiveNormalizationSynapsePanel();
            }
        };
        
        creatorsByName.put(SubtractiveNormalizationSynapse.getName(), creator);
        creatorsByClass.put(SubtractiveNormalizationSynapse.class, creator);

        creator = new Creator() {
            public String getName() {
                return ClampedSynapse.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new ClampedSynapse(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new ClampedSynapsePanel();
            }
        };
        
        creatorsByName.put(ClampedSynapse.getName(), creator);
        creatorsByClass.put(ClampedSynapse.class, creator);

        creator = new Creator() {
            public String getName() {
                return ShortTermPlasticitySynapse.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new ShortTermPlasticitySynapse(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new ShortTermPlasticitySynapsePanel();
            }
        };
        
        creatorsByName.put(ShortTermPlasticitySynapse.getName(), creator);
        creatorsByClass.put(ShortTermPlasticitySynapse.class, creator);

        creator = new Creator() {
            public String getName() {
                return HebbianThresholdSynapse.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new HebbianThresholdSynapse(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new HebbianThresholdSynapsePanel();
            }
        };
        
        creatorsByName.put(HebbianThresholdSynapse.getName(), creator);
        creatorsByClass.put(HebbianThresholdSynapse.class, creator);

        creator = new Creator() {
            public String getName() {
                return SignalSynapse.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new SignalSynapse(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new SignalSynapsePanel();
            }
        };
        
        creatorsByName.put(SignalSynapse.getName(), creator);
        creatorsByClass.put(SignalSynapse.class, creator);

        creator = new Creator() {
            public String getName() {
                return SimpleSynapse.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new SimpleSynapse(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new SimpleSynapsePanel();
            }
        };
        
        creatorsByName.put(SimpleSynapse.getName(), creator);
        creatorsByClass.put(SimpleSynapse.class, creator);

        creator = new Creator() {
            public String getName() {
                return TraceSynapse.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new TraceSynapse(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new TraceSynapsePanel();
            }
        };

        creatorsByName.put(TraceSynapse.getName(), creator);
        creatorsByClass.put(TraceSynapse.class, creator);

        creator = new Creator() {
            public String getName() {
                return STDPSynapse.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new STDPSynapse(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new STDPSynapsePanel();
            }
        };

        creatorsByName.put(STDPSynapse.getName(), creator);
        creatorsByClass.put(STDPSynapse.class, creator);

        creator = new Creator() {
            public String getName() {
                return HebbianCPCA.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new HebbianCPCA(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new HebbianCPCAPanel();
            }
        };
        
        creatorsByName.put(HebbianCPCA.getName(), creator);
        creatorsByClass.put(HebbianCPCA.class, creator);

        creator = new Creator() {
            public String getName() {
                return TDSynapse.getName();
            }
            
            public Synapse createSynapse(Synapse old) {
                return new TDSynapse(old);
            }

            public AbstractSynapsePanel createPanel() {
                return new TDSynapsePanel();
            }
        };
        
        creatorsByName.put(TDSynapse.getName(), creator);
        creatorsByClass.put(TDSynapse.class, creator);
    }

    /**
     * @return the synapseList
     */
    public List<Synapse> getSynapseList() {
        return synapseList;
    }

    /**
     * @param synapseList the synapseList to set
     */
    public void setSynapseList(List<Synapse> synapseList) {
        this.synapseList = synapseList;
    }
}