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

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.util.ClassDescriptionPair;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.ShowHelpAction;
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

    /** Scrollpane for neuron panel. */
    private JScrollPane scrollPane;

    /** Neuron type combo box. */
    private JComboBox cbNeuronType = new JComboBox(Neuron.getRulelist());

    /** Id Label. */
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
    private ShowHelpAction helpAction;

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
        // if (((Neuron) neuronList.get(0)).getNetwork().getUpdateMethod() !=
        // Network.UpdateMethod.PRIORITYBASED) {
        // priorityLabel.setEnabled(false);
        // tfPriority.setEnabled(false);
        // }

        // topPanel.addItem("Attribute type", cbNeuronType);
        topPanel.addItem("Update rule", cbNeuronType);

        mainPanel.add(topPanel);

        initScrollPane(neuronPanel);
        mainPanel.add(scrollPane);
        setContentPane(mainPanel);

    }

    /**
     * Initialize the scroll panel: set its properties and re-init the scrollbar
     * policy. This is called every time a new neuron type is selected, so that
     * the panel can be laid out again.
     *
     * @param npanel
     */
    private void initScrollPane(JPanel npanel) {
        scrollPane = new JScrollPane(npanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    }


    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Initialize the main neuron panel based on the type of the selected
     * neurons.
     */
    private void initNeuronType() {

        Network parentNetwork = neuronList.get(0).getNetwork();
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getType")) {
            cbNeuronType.addItem(AbstractNeuronPanel.NULL_STRING);
            cbNeuronType.setSelectedIndex(cbNeuronType.getItemCount() - 1);
            // Simply to serve as an empty panel
            neuronPanel = new ClampedNeuronRulePanel(parentNetwork);
        } else {
            setComboBox(neuronList.get(0).getUpdateRule().getDescription());
            Class<?> neuronType = ((ClassDescriptionPair) cbNeuronType
                    .getSelectedItem()).getTheClass();
            neuronPanel = getNeuronPanel(parentNetwork, neuronType);
            neuronPanel.setRuleList(getRuleList());
            neuronPanel.fillFieldValues();
        }
    }

    /**
     * Utility for setting the selected item of a combo box based on a neuron's
     * update rule description.
     */
    private void setComboBox(final String description) {
        for (int i = 0; i < cbNeuronType.getItemCount(); i++) {
            ClassDescriptionPair pair = (ClassDescriptionPair) cbNeuronType
                    .getItemAt(i);
            if (pair.getDescription().equalsIgnoreCase(description)) {
                cbNeuronType.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Returns neuron panel corresponding to the given update rule. Assumes the
     * panel class name = update rule class name + "Panel" E.g. "LinearNeuron" >
     * "LinearNeuronPanel".
     *
     * @param updateRuleClass the class to match
     * @return panel the matching panel
     */
    private AbstractNeuronPanel getNeuronPanel(Network network,
            Class<?> updateRuleClass) {
        // The panel name to look for
        String panelClassName = "org.simbrain.network.gui.dialogs.neuron."
                + updateRuleClass.getSimpleName() + "Panel";

        try {
            Class<?> theClass = Class.forName(panelClassName);
            Constructor<?> constructor = theClass
                    .getConstructor(new Class[] { Network.class });
            return (AbstractNeuronPanel) constructor.newInstance(network);
        } catch (ClassNotFoundException e) {
            System.err.print("The class, \"" + panelClassName
                    + "\", was not found.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Return the neuron update rules associated with the selected neurons.
     *
     * @return the rule list.
     */
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
        if (selected != NULL_STRING) {
            String name = ((ClassDescriptionPair) selected).getSimpleName();
            for (int i = 0; i < neuronList.size(); i++) {
                neuronList.get(i).setUpdateRule(name);
            }
        }
    }

    /**
     * Set the help page based on the currently selected neuron type. Assumes it
     */
    private void updateHelp() {
        if (cbNeuronType.getSelectedItem() == NULL_STRING) {
            helpAction = new ShowHelpAction("Pages/Network/neuron.html");
        } else {
            String name = ((ClassDescriptionPair) cbNeuronType
                    .getSelectedItem()).getSimpleName().replaceAll("Rule", "");
            name = name.substring(0, 1).toLowerCase().concat(name.substring(1));
            helpAction = new ShowHelpAction("Pages/Network/neuron/" + name
                    + ".html");
        }
        helpButton.setAction(helpAction);
    }

    /**
     * Respond to neuron type changes.
     *
     * @param e Action event.
     */
    private final ActionListener listener = new ActionListener() {

        public void actionPerformed(final ActionEvent e) {
            neuronsHaveChanged = true;
            Network network = neuronList.get(0).getNetwork();
            updateHelp();

            Object selected = cbNeuronType.getSelectedItem();
            if (selected == NULL_STRING) {
                return;
            }

            mainPanel.remove(scrollPane);
            neuronPanel = getNeuronPanel(network,
                    ((ClassDescriptionPair) selected).getTheClass());
            neuronPanel.fillDefaultValues();
            initScrollPane(neuronPanel);
            mainPanel.add(scrollPane);
            scrollPane.revalidate();
            pack();
            centerDialog();
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
        tfPriority.setText(Integer.toString(neuronRef.getUpdatePriority()));
        tfLowBound.setText(Double.toString(neuronRef.getLowerBound()));
        tfUpBound.setText(Double.toString(neuronRef.getUpperBound()));

        neuronPanel.fillFieldValues();

        // Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
                "getActivation")) {
            tfActivation.setText(NULL_STRING);
        }

        if (!NetworkUtils
                .isConsistent(neuronList, Neuron.class, "getIncrement")) {
            tfIncrement.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getLabel")) {
            tfNeuronLabel.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
                "getUpdatePriority")) {
            tfPriority.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
                "getLowerBound")) {
            tfLowBound.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
                "getUpperBound")) {
            tfUpBound.setText(NULL_STRING);
        }
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {

        for (int i = 0; i < neuronList.size(); i++) {

            Neuron neuronRef = neuronList.get(i);

            if (!tfActivation.getText().equals(NULL_STRING)) {
                neuronRef.setActivation(Double.parseDouble(tfActivation
                        .getText()));
            }

            if (!tfIncrement.getText().equals(NULL_STRING)) {
                neuronRef
                        .setIncrement(Double.parseDouble(tfIncrement.getText()));
            }

            if (!tfNeuronLabel.getText().equals(NULL_STRING)) {
                neuronRef.setLabel(tfNeuronLabel.getText());
            }

            if (!tfPriority.getText().equals(NULL_STRING)) {
                neuronRef.setUpdatePriority(Integer.parseInt(tfPriority
                        .getText()));
            }

            if (!tfUpBound.getText().equals(NULL_STRING)) {
                neuronRef
                        .setUpperBound(Double.parseDouble(tfUpBound.getText()));
            }

            if (!tfLowBound.getText().equals(NULL_STRING)) {
                neuronRef
                        .setLowerBound(Double.parseDouble(tfLowBound.getText()));
            }

        }

        if (neuronsHaveChanged) {
            changeNeuronTypes();
        }

        // Notify the network that changes have been made
        neuronList.get(0).getParentNetwork().fireNetworkChanged();

        // Now commit changes specific to the neuron type
        neuronPanel.setRuleList(getRuleList());
        neuronPanel.commitChanges();
    }

}
