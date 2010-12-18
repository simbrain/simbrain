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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.actions.ShowHelpAction;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.NeuronUpdateRule;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>NeuronDialog</b> is a dialog box for setting the properties of a Neuron.
 */
public class NeuronDialog extends StandardDialog {

    /** The default serial version id. */
    private static final long serialVersionUID = 1L;

    /** Null string. */
    public static final String NULL_STRING = "...";

    /** Main panel. */
    private Box mainPanel = Box.createVerticalBox();

    /** Top panel. */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /** Neuron panel. */
    private AbstractNeuronPanel neuronPanel;

    /** Neuron type combo box. */
    private JComboBox cbNeuronType = new JComboBox(Neuron.ruleList);

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

    /** Priority Field. */
    private JTextField tfPriority = new JTextField();

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

        // For update priority, make it enabled or disabled based on current
        // update method
        JLabel priorityLabel = new JLabel("Update Priority");
        topPanel.addItemLabel(priorityLabel, tfPriority);
        if (((Neuron) neuronList.get(0)).getParentNetwork().getRootNetwork().getUpdateMethod() != 
            RootNetwork.UpdateMethod.PRIORITYBASED) {
            priorityLabel.setEnabled(false);
            tfPriority.setEnabled(false);
        }

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
     * Initialize the main neuron panel based on the type of the selected
     * neurons.
     */
    public void initNeuronType() {

        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getType")) {
            cbNeuronType.addItem(AbstractNeuronPanel.NULL_STRING);
            cbNeuronType.setSelectedIndex(cbNeuronType.getItemCount() - 1);
            // Simply to serve as an empty panel
            neuronPanel = new ClampedNeuronPanel();
        } else {
            Neuron neuronRef = (Neuron) neuronList.get(0);
            String name = getNeuronType(neuronRef);

            cbNeuronType.setSelectedItem(name);

            neuronPanel = getNeuronPanel(name);
            neuronPanel.setParentNetwork(neuronRef.getParentNetwork().getRootNetwork());
            neuronPanel.setRuleList(getRuleList());
            neuronPanel.fillFieldValues();
        }
    }

    //TODO Better documentation
    //      assumes class-name + panel

    private String getNeuronType(Neuron neuron) {
        return  ((NeuronUpdateRule) neuron.getUpdateRule()).getClass().getSimpleName().replaceAll("Neuron", "");
    }

    /**
     * Returns neuron panel corresponding to the given name.
     *
     * @param neuron neuron
     * @return panel
     */
    private AbstractNeuronPanel getNeuronPanel(String name) {
        try {
            return (AbstractNeuronPanel)Class.forName("org.simbrain.network.gui.dialogs.neuron." + name + "NeuronPanel").newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayList<NeuronUpdateRule> getRuleList() {
        ArrayList<NeuronUpdateRule> ret = new ArrayList<NeuronUpdateRule>();
        for (Neuron neuron : neuronList) {
            ret.add(neuron.getUpdateRule());
        }
        return ret;
    }

    /**
     * Change all the neurons from their current type to the new selected type.
     */
    private void changeNeuronTypes() {

        Object selected = cbNeuronType.getSelectedItem();

        if (selected == NULL_STRING) { return; }

        for (int i = 0; i < neuronList.size(); i++) {
            neuronList.get(i).setUpdateRule(selected.toString() + "Neuron");
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
     *
     * @param e Action event.
     */
    private final ActionListener listener = new ActionListener() {

        public void actionPerformed(final ActionEvent e) {
            neuronsHaveChanged = true;
            Neuron neuronRef = (Neuron) neuronList.get(0);
            updateHelp();

            Object selected = cbNeuronType.getSelectedItem();
            if (selected == NULL_STRING) {
                return;
            }

            mainPanel.remove(neuronPanel);
            neuronPanel = getNeuronPanel((String)selected);
            neuronPanel.setParentNetwork(neuronRef.getParentNetwork().getRootNetwork());
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
        neuronPanel.setParentNetwork(neuronRef.getParentNetwork().getRootNetwork());
        if (neuronList.size() == 1) {
            idLabel.setText(neuronRef.getId());
        } else {
            idLabel.setText(NULL_STRING);
        }
        tfActivation.setText(Double.toString(neuronRef.getActivation()));
        tfIncrement.setText(Double.toString(neuronRef.getIncrement()));
        tfNeuronLabel.setText(neuronRef.getLabel());
        tfPriority.setText(Integer.toString(neuronRef.getUpdatePriority()));
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

        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getUpdatePriority")) {
            tfPriority.setText(NULL_STRING);
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

            if (!tfPriority.getText().equals(NULL_STRING)) {
                neuronRef.setUpdatePriority(Integer.parseInt(tfPriority .getText()));
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
        neuronPanel.setRuleList(getRuleList());
        neuronPanel.commitChanges();
    }


}
