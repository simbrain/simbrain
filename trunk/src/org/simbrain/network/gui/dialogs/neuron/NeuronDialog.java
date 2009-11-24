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
package org.simbrain.network.gui.dialogs.neuron;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.actions.ShowHelpAction;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.neurons.AdditiveNeuron;
import org.simbrain.network.neurons.BinaryNeuron;
import org.simbrain.network.neurons.ClampedNeuron;
import org.simbrain.network.neurons.CustomNeuron;
import org.simbrain.network.neurons.DecayNeuron;
import org.simbrain.network.neurons.ExponentialDecayNeuron;
import org.simbrain.network.neurons.IACNeuron;
import org.simbrain.network.neurons.IntegrateAndFireNeuron;
import org.simbrain.network.neurons.IzhikevichNeuron;
import org.simbrain.network.neurons.LMSNeuron;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.neurons.LogisticNeuron;
import org.simbrain.network.neurons.NakaRushtonNeuron;
import org.simbrain.network.neurons.PointNeuron;
import org.simbrain.network.neurons.RandomNeuron;
import org.simbrain.network.neurons.RunningAverageNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron;
import org.simbrain.network.neurons.SinusoidalNeuron;
import org.simbrain.network.neurons.StochasticNeuron;
import org.simbrain.network.neurons.TemporalDifferenceNeuron;
import org.simbrain.network.neurons.ThreeValuedNeuron;
import org.simbrain.network.neurons.TraceNeuron;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>NeuronDialog</b> is a dialog box for setting the properties of the Neuron.
 */
public class NeuronDialog extends StandardDialog {

    /** The default serial version id. */
    private static final long serialVersionUID = 1L;

    /** The neuron types indexed by name. */
    private static final Map<Class<? extends Neuron>, Association> ASSOCIATIONS
        = new LinkedHashMap<Class<? extends Neuron>, Association>();
    
    /* Temporary fill of associations.  To be replaced with load from file. */
    static {
        Association association;
        
        association = new Association("Additive (Continuous Hopfield)", AdditiveNeuron.class,
                AdditiveNeuronPanel.class, true);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Binary", BinaryNeuron.class,
                BinaryNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);
       
        association = new Association("Clamped", ClampedNeuron.class,
                ClampedNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Custom", CustomNeuron.class,
                CustomNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Decay", DecayNeuron.class, DecayNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Exponential decay", ExponentialDecayNeuron.class,
                ExponentialDecayNeuronPanel.class, true);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("IAC", IACNeuron.class,
                IACNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Integrate and fire", IntegrateAndFireNeuron.class,
                IntegrateAndFireNeuronPanel.class, true);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Izhikevich", IzhikevichNeuron.class,
                IzhikevichNeuronPanel.class, true);
        ASSOCIATIONS.put(association.clazz, association);
 
        association = new Association("Linear", LinearNeuron.class,
                LinearNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("LMS", LMSNeuron.class, LMSNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);
      
        association = new Association("Logistic", LogisticNeuron.class,
                LogisticNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Naka-Rushton", NakaRushtonNeuron.class,
                NakaRushtonNeuronPanel.class, true);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Point", PointNeuron.class, PointNeuronPanel.class, true);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Random", RandomNeuron.class, RandomNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Running Average", RunningAverageNeuron.class,
                RunningAverageNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Sigmoidal", SigmoidalNeuron.class,
                SigmoidalNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);
                
        association = new Association("Sinusoidal", SinusoidalNeuron.class,
                SinusoidalNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Stochastic", StochasticNeuron.class,
                StochasticNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);
             
        association = new Association("Temporal Difference", TemporalDifferenceNeuron.class,
                TemporalDifferenceNeuronPanel.class, false);
            ASSOCIATIONS.put(association.clazz, association);

        association = new Association("Three valued", ThreeValuedNeuron.class,
            ThreeValuedNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);
        
        association = new Association("Trace", TraceNeuron.class, TraceNeuronPanel.class, false);
        ASSOCIATIONS.put(association.clazz, association);

    }
    
    /** Null string. */
    public static final String NULL_STRING = "...";

    /** Main panel. */
    private Box mainPanel = Box.createVerticalBox();

    /** Top panel. */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /** Neuron panel. */
    private AbstractNeuronPanel neuronPanel;

    /** Neuron type combo box. */
    private JComboBox cbNeuronType = new JComboBox(ASSOCIATIONS.values().toArray());

    /** Id Field. */
    private JLabel idLabel = new JLabel();

    /** Activation field. */
    private JTextField tfActivation = new JTextField();

    /** Increment field. */
    private JTextField tfIncrement = new JTextField();

    /** Upper bound field. */
    private JTextField tfUpBound = new JTextField();

    /** Lower bound field. */
    private JTextField tfLowBound = new JTextField();
    
    /** Label Field. */
    private JTextField tfNeuronLabel = new JTextField();

    /** Upper label. */
    private JLabel upperLabel = new JLabel("Upper bound");

    /** Lower label. */
    private JLabel lowerLabel = new JLabel("Lower bound");

    /** Help Button. */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. */
    private ShowHelpAction helpAction = new ShowHelpAction();

    /** The neurons being modified. */
    private ArrayList<Neuron> neuronList = new ArrayList<Neuron>();

    /** The pnodes which refer to them. */
    private ArrayList<NeuronNode> selectionList;

    /** Used to determine if anything in the workspace has been changed. */
    private boolean neuronsHaveChanged = false;

    /**
     * @param selectedNeurons the pnode_neurons being adjusted
     */
    public NeuronDialog(final Collection<NeuronNode> selectedNeurons) {
        selectionList = new ArrayList<NeuronNode>(selectedNeurons);
        setNeuronList();
        init();
    }

    /**
     * Get the logical neurons from the NeuronNodes.
     */
    private void setNeuronList() {
        neuronList.clear();

        for (NeuronNode n : selectionList) {
            neuronList.add(n.getNeuron());
        }
    }

    /**
     * Initializes the components on the panel.
     */
    private void init() {
        setTitle("Neuron Dialog");

        initNeuronType();
        fillFieldValues();
        updateHelp();

        helpButton.setAction(helpAction);
        this.addButton(helpButton);
        cbNeuronType.addActionListener(listener);

        topPanel.addItem("Id:", idLabel);
        topPanel.addItem("Activation", tfActivation);
        topPanel.addItemLabel(upperLabel, tfUpBound);
        topPanel.addItemLabel(lowerLabel, tfLowBound);
        topPanel.addItem("Adjustment Increment", tfIncrement);
        topPanel.addItem("Label (Optional)", tfNeuronLabel);
        //topPanel.addItem("Attribute type", cbNeuronType);
        topPanel.addItem("Neuron type", cbNeuronType);

        mainPanel.add(topPanel);
        mainPanel.add(neuronPanel);
        setContentPane(mainPanel);

    }

    /** @see StandardDialog */
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }
    
    /**
     * Initialize the main neuron panel based on the type of the selected neurons.
     */
    public void initNeuronType() {
        
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getType")) {
            cbNeuronType.addItem(AbstractNeuronPanel.NULL_STRING);
            cbNeuronType.setSelectedIndex(cbNeuronType.getItemCount() - 1);
            neuronPanel = new ClampedNeuronPanel(); // Simply to serve as an empty panel
        } else {
            Neuron neuronRef = (Neuron) neuronList.get(0);
            Association association = ASSOCIATIONS.get(
                (Class<? extends Neuron>) neuronRef.getClass());
            
            if (association == null) {
                throw new IllegalStateException("unknown neuron type: "
                    + cbNeuronType.getSelectedItem());
            }
            
            cbNeuronType.setSelectedItem(association);
            neuronPanel = association.getPanel(neuronRef.getParentNetwork());
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        }
    }

    /**
     * Change all the neurons from their current type to the new selected type.
     */
    public void changeNeuronTypes() {
        
        Object selected = cbNeuronType.getSelectedItem();
        
        if (selected == NULL_STRING) { return; }
        
        Association association = (Association) selected;
                
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron oldNeuron = (Neuron) neuronList.get(i);
            Neuron newNeuron = association.getNeuron(oldNeuron);
            newNeuron.getParentNetwork().changeNeuronType(oldNeuron, newNeuron);
        }
    }

    /**
     * Set the help page based on the currently selected neuron type.
     */
    private void updateHelp() {
        if (cbNeuronType.getSelectedItem() == NULL_STRING) {
            helpAction.setTheURL("Network/neuron.html");
        } else {
            String spacelessString = cbNeuronType.getSelectedItem().toString().replace(" ", "");
            helpAction.setTheURL("Network/neuron/" + spacelessString + ".html");
        }
    }
    
    /**
     * Respond to neuron type changes.
     * @param e Action event.
     */
    private final ActionListener listener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            neuronsHaveChanged = true;
            Neuron neuronRef = (Neuron) neuronList.get(0);
            updateHelp();
    
            Object selected = cbNeuronType.getSelectedItem();
            
            if (selected == NULL_STRING) { return; }
            
            Association association = (Association) selected;
            
            mainPanel.remove(neuronPanel);
            neuronPanel = association.getPanel(neuronRef.getParentNetwork());
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
            
            pack();
        }
    };

    /**
     * Set the initial values of dialog components.
     */
    private void fillFieldValues() {
        Neuron neuronRef = neuronList.get(0);
        if (neuronList.size() == 1) {
            idLabel.setText(neuronRef.getId());
        } else {
            idLabel.setText(NULL_STRING);
        }
        tfActivation.setText(Double.toString(neuronRef.getActivation()));
        tfIncrement.setText(Double.toString(neuronRef.getIncrement()));
        tfNeuronLabel.setText(neuronRef.getLabel());
        tfLowBound.setText(Double.toString(neuronRef.getLowerBound()));
        tfUpBound.setText(Double.toString(neuronRef.getUpperBound()));

        neuronPanel.fillFieldValues();

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getActivation")) {
            tfActivation.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getIncrement")) {
            tfIncrement.setText(NULL_STRING);
        }
        
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getLabel")) {
            tfNeuronLabel.setText(NULL_STRING);
        }
        
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getLowerBound")) {
            tfLowBound.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getUpperBound")) {
            tfUpBound.setText(NULL_STRING);
        }
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {

        for (int i = 0; i < neuronList.size(); i++) {
            Neuron neuronRef = (Neuron) neuronList.get(i);

            if (!tfActivation.getText().equals(NULL_STRING)) {
                neuronRef.setActivation(Double.parseDouble(tfActivation.getText()));
            }

            if (!tfIncrement.getText().equals(NULL_STRING)) {
                neuronRef.setIncrement(Double.parseDouble(tfIncrement.getText()));
            }
            
            if (!tfNeuronLabel.getText().equals(NULL_STRING)) {
                neuronRef.setLabel(tfNeuronLabel.getText());
            }
            
            if (!tfUpBound.getText().equals(NULL_STRING)) {
                neuronRef.setUpperBound(Double.parseDouble(tfUpBound.getText()));
            }

            if (!tfLowBound.getText().equals(NULL_STRING)) {
                neuronRef.setLowerBound(Double.parseDouble(tfLowBound.getText()));
            }

        }

        if (neuronsHaveChanged) {
            changeNeuronTypes();
        }

        ((Neuron) neuronList.get(0)).getParentNetwork().getRootNetwork().fireNetworkChanged();
        setNeuronList();
        neuronPanel.setNeuronList(neuronList);
        neuronPanel.commitChanges();
    }
    
    /**
     * Holds information about a neuron and associated properties.
     * 
     * @author Matt Watson
     */
    private static class Association {
        /** The display name for the neuron type. */
        private final String name;
        /** The neuron's class. */
        private final Class<? extends Neuron> clazz;
        /** The neuron's constructor. */
        private final Constructor<? extends Neuron> neuronConstructor;
        /** The neuron's panel constructor. */
        private final Constructor<? extends AbstractNeuronPanel> panelConstructor;
        /** Whether the panel constructor takes a parent network. */
        private final boolean withParent;
        
        /**
         * Creates a new association.
         * 
         * @param name The display name for the neuron type.
         * @param neuronClass The neuron's class.
         * @param panelClass The neuron's panel class.
         * @param withParent Whether the panel constructor takes a parent network.
         */
        Association(final String name, final Class<? extends Neuron> neuronClass,
                final Class<? extends AbstractNeuronPanel> panelClass, final boolean withParent) {
            this.name = name;
            this.clazz = neuronClass;
            this.withParent = withParent;
            
            try {
                this.neuronConstructor = neuronClass.getConstructor(Neuron.class);
                this.panelConstructor = withParent
                    ? panelClass.getConstructor(Network.class)
                    : panelClass.getConstructor();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        /**
         * Creates a new panel.
         * 
         * @param parent Passed to the constructor if needed.
         * @return a new panel instance.
         */
        AbstractNeuronPanel getPanel(final Network parent) {
            try {
                return withParent ? panelConstructor.newInstance(parent)
                    : panelConstructor.newInstance();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        /**
         * Creates a new neuron from the old neuron.
         * 
         * @param old The old neuron.
         * @return A new neuron.
         */
        Neuron getNeuron(final Neuron old) {
            try {
                return neuronConstructor.newInstance(old);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        /**
         * {@inheritDoc}
         */
        public String toString() {
            return name;
        }
    }
}
