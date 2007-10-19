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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

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
import org.simnet.synapses.SimpleSynapse;
import org.simnet.synapses.SubtractiveNormalizationSynapse;
import org.simnet.synapses.TDSynapse;
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

    /** Upper label. */
    private JLabel upperLabel = new JLabel("Upper bound");

    /** Lower label. */
    private JLabel lowerLabel = new JLabel("Lower bound");

    /** Synapse type combo box. */
    private JComboBox cbSynapseType = new JComboBox(Synapse.getTypeList());

    /** The synapses being modified. */
    private ArrayList synapseList = new ArrayList();

    /** The pnodes which refer to them. */
    private ArrayList<SynapseNode> selectionList;

    /** Weights have changed boolean. */
    private boolean weightsHaveChanged = false;

    /** Help Button. */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. */
    private ShowHelpAction helpAction = new ShowHelpAction();

    private Map<String, Creator> creatorsByName = new TreeMap<String, Creator>(String.CASE_INSENSITIVE_ORDER);
    private Map<Class, Creator> creatorsByClass = new HashMap<Class, Creator>();

    /**
     * This method is the default constructor.
     * @param selectedSynapses LIst of synapses that are selected
     */
    public SynapseDialog(final Collection<SynapseNode> selectedSynapses) {
        initializeCreators();
        selectionList = new ArrayList<SynapseNode>(selectedSynapses);
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

        mainPanel.add(topPanel);
        mainPanel.add(synapsePanel);

        ArrayList spikeResponders = getSpikeResponders();

        if (spikeResponders.size() > 0) {
            spikeResponsePanel = new SpikeResponsePanel(spikeResponders, this);
            tabbedPane.addTab("Synaptic Efficacy", mainPanel);
            tabbedPane.addTab("Spike Response", spikeResponsePanel);
            setContentPane(tabbedPane);
            updateHelp();
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
        } else {
            Creator creator = creatorsByClass.get(synapseRef.getClass());
            
            if (creator == null) return;
            
            cbSynapseType.setSelectedIndex(Synapse.getSynapseTypeIndex(creator.getName()));
            synapsePanel = creator.createPanel();
        }
        
        synapsePanel.setSynapseList(synapseList);
        synapsePanel.fillFieldValues();        
    }

    private static interface Creator {
        String getName();
        Synapse createSynapse(Synapse old);
        AbstractSynapsePanel createPanel(); 
    }
    
    /**
     * Change all the synapses from their current type  to the new selected type.
     */
    public void changeSynapses() {
        Creator creator = creatorsByName.get(cbSynapseType.getSelectedItem().toString());
        
        if (creator == null) return;
        
        for (int i = 0; i < synapseList.size(); i++) {
            Synapse oldSynapse = (Synapse) synapseList.get(i);
            Synapse newSynapse = creator.createSynapse(oldSynapse);
            oldSynapse.getSource().getParentNetwork().changeSynapse(oldSynapse, newSynapse);
        }
    }

    /**
     * Respond to synapse type changes.
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
}