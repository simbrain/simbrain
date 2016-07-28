/*
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
package org.simbrain.network.gui.dialogs.group;

import java.awt.GridLayout;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.util.widgets.EditablePanel;

/**
 * A panel which provides a high level summary of certain groups in Simbrain. As
 * of right now, Subnetworks are accepted, but only NeuronGroups and
 * SynapseGroups actually have implementations in this class.
 *
 * @author ztosi
 */
@SuppressWarnings("serial")
public class SummaryPanel extends JPanel implements EditablePanel {

    /** The default name for this type of panel. */
    public static final String DEFAULT_PANEL_NAME = "Summary";

    /** The default vertical gap between components of this panel. */
    private static final int DEFAULT_VGAP = 5; // px

    /** The default horizontal gap between components of this panel. */
    private static final int DEFAULT_HGAP = 50; // px

    /** The name of the panel. */
    private String name = DEFAULT_PANEL_NAME;

    /** The group this panel is summarizing. */
    private Group group;

    /** A flag for whether or not the population is editable from this panel. */
    private boolean editable;

    /** Id label */
    private final JLabel idLabel = new JLabel("Group ID: ");

    /** Label label */
    private final JLabel nameLabel = new JLabel("Label: ");

    /** Population label */
    private final JLabel populationLabel = new JLabel("Population: ");

    /** Excitatory type label */
    private final JLabel excitatoryTypeLabel = new JLabel("Excitatory Type: ");

    /** Inhibitory type label */
    private final JLabel inhibitoryTypeLabel = new JLabel("Inhibitory Type: ");

    /** A label for the parent group. */
    private final JLabel parentGroupLabel = new JLabel("Parent: ");

    /** A label for any incoming groups. */
    private final JLabel incomingGroupLabel = new JLabel();

    /** A label for any outgoing groups. */
    private final JLabel outgoingGroupLabel = new JLabel();

    /** A label for synapse group learning rate. */
    private final JLabel learningRateLabel = new JLabel("Learning rate:");

    /** The field for the group id. */
    private JLabel idField = new JLabel();

    /** The field for the name/label of the group. */
    private JTextField nameField = new JTextField();

    /** The population of the group field. */
    private JLabel populationField = new JLabel();

    /**
     * An editable population field for situations (creation) where population
     * can be edited.
     */
    private JTextField editablePopulationField = new JTextField();

    /** The excitatory plasticity rule contained in the group field. */
    private JLabel excitatoryTypeField = new JLabel();

    /** The inhibitory plasticity rule contained in the group field. */
    private JLabel inhibitoryTypeField = new JLabel();

    /** A field for displaying the parent group of this group if it exists. */
    private JLabel parentGroupField = new JLabel();

    /**
     * For NeuronGroups, the name of the incoming synapse group if it exists.
     * For SynapseGroups, the name of the pre-synaptic neuron group.
     */
    private JLabel incomingField = new JLabel();

    /**
     * For NeuronGroups, the name of the outgoing synapse group if it exists.
     * For SynapseGroups, the name of the post-synaptic neuron group.
     */
    private JLabel outgoingField = new JLabel();

    /** The field for learning rate. */
    private JTextField learningRateField = new JTextField();

    private JCheckBox useGlobalSettings = new JCheckBox();
    
    private JCheckBox inputMode = new JCheckBox();
    {
        inputMode.setSelected(false);
    }
    
    /**
     * Constructs the summary panel based on a neuron group. Names the incoming
     * and outgoing labels appropriately (Incoming/Outgoing).
     *
     * @param ng
     *            the neuron group for which a summary will be built
     * @param editable
     *            is this summary panel's population field editable? This
     *            equates to asking if this summary panel is being used for
     *            creating (true) or editing a neuron group (false).
     */
    public SummaryPanel(final NeuronGroup ng, boolean editable) {
        setGroup(ng);
        this.editable = editable;
        incomingGroupLabel.setText("Incoming: ");
        outgoingGroupLabel.setText("Outgoing: ");
        fillFieldValues();
        initializeLayout();
    }

    /**
     * Constructs the summary panel based on a synapse group. Names the incoming
     * and outgoing labels appropriately (Source Group/Target Group).
     *
     * @param sg
     *            the synapse group for which a summary will be built
     */
    public SummaryPanel(final SynapseGroup sg) {
        setGroup(sg);
        incomingGroupLabel.setText("Source Group: ");
        outgoingGroupLabel.setText("Target Group: ");
        fillFieldValues();
        initializeLayout();
    }

    /**
     * Initialize the layout.
     */
    private void initializeLayout() {
        GridLayout layout = new GridLayout(0, 2);
        layout.setVgap(DEFAULT_VGAP);
        layout.setHgap(DEFAULT_HGAP);
        this.setLayout(layout);
        this.add(idLabel);
        this.add(idField);
        this.add(nameLabel);
        this.add(nameField);
        this.add(populationLabel);
        if (isEditable()) {
            this.add(editablePopulationField);
        } else {
            this.add(populationField);
        }
        if (group instanceof SynapseGroup) {
            this.add(new JLabel("Optimize as Group:"));
            this.add(useGlobalSettings);
        }
        if (group instanceof NeuronGroup && !editable) {
            this.add(new JLabel("Input Mode:"));
            this.add(inputMode);
        }

        this.add(excitatoryTypeLabel);
        this.add(excitatoryTypeField);
        if (group instanceof SynapseGroup || inhibitoryTypeLabel.isVisible()) {
        	this.add(inhibitoryTypeLabel);
        	this.add(inhibitoryTypeField);
        }
        if (!isEditable()) {
        	this.add(parentGroupLabel);
        	this.add(parentGroupField);
        	if (group instanceof SynapseGroup
        			|| inhibitoryTypeLabel.isVisible()) {
        		this.add(incomingGroupLabel);
        		this.add(incomingField);
        		this.add(outgoingGroupLabel);
        		this.add(outgoingField);
        	}
        }
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    /**
     *
     * @param ng
     *            the neuron group being used to fill the values of this panel's
     *            fields.
     */
    public void fillFieldValues(final NeuronGroup ng) {
    	// TODO: See below:
    	// Temporary fix code. These values would be very difficult to display
    	// in the current framework. Basically we're wating until neuron groups'
    	// synapse group sets are actually used and we're waiting until neuron
    	// polarity can be set in the GUI.
    	incomingGroupLabel.setVisible(false);
    	outgoingGroupLabel.setVisible(false);
    	incomingField.setVisible(false);
    	outgoingField.setVisible(false);
    	inhibitoryTypeLabel.setVisible(false);
    	inhibitoryTypeField.setVisible(false);
        excitatoryTypeLabel.setText("Neuron Type");
        removeAll();
    	// End Temporary fixes.
    	inputMode.setSelected(ng.isInputMode());
        if (ng.getId() == null || ng.getId().isEmpty()) {
            idField.setText(ng.getParentNetwork().getGroupIdGenerator()
                .getHypotheticalId()); // ng hasn't been added to the
                                       // network
            // yet
        } else {
            idField.setText(ng.getId());
        }
        if (ng.getLabel() == null || ng.getLabel().isEmpty()) {
            nameField.setText(idField.getText());
        } else {
            nameField.setText(ng.getLabel());
        }
        if (ng.getNeuronList().size() > 0) {
            if (isEditable()) {
                editablePopulationField.setText(Integer.toString(ng
                    .getNeuronList().size()));
            } else {
                populationField.setText(Integer.toString(ng.getNeuronList()
                    .size()));
            }

            if (NetworkUtils.isConsistent(ng.getNeuronList(), Neuron.class,
                "getUpdateRuleDescription")) {
                excitatoryTypeField.setText(ng.getNeuronList().get(0)
                    .getUpdateRule().getName());
            } else {
                excitatoryTypeField.setText("Mixed");
            }
        } else {
            if (isEditable()) {
                // Is a creation dialog
                editablePopulationField.setText(Integer
                    .toString(NeuronGroup.DEFAULT_GROUP_SIZE));
                excitatoryTypeField.setText(Neuron.DEFAULT_UPDATE_RULE
                    .getName());
            } else {
                // Handles if an empty neuron group is selected
                // (for some reason)...
                populationField.setText(Integer.toString(0));
                excitatoryTypeField.setText("None");
            }
        }
        parentGroupField.setText(ng.getParentGroup() == null ? "None" : ng
            .getParentGroup().getLabel());
        // if (ng.getIncomingSg() == null) {
        // if (ng.getIncomingWeights() != null
        // && ng.getIncomingWeights().size() > 0)
        // {
        // incomingField.setText("Loose (un-grouped) synapses");
        // } else {
        // incomingField.setText("None");
        // }
        // } else {
        // incomingField.setText(ng.getIncomingSg().getLabel());
        // }
        // if (ng.getOutgoingSg() == null) {
        // if (ng.getOutgoingWeights() != null
        // && ng.getOutgoingWeights().size() > 0)
        // {
        // outgoingField.setText("Loose (un-grouped) synapses");
        // } else {
        // outgoingField.setText("None");
        // }
        // } else {
        // outgoingField.setText(ng.getIncomingSg().getLabel());
        // }
    }

    /**
     *
     * @param sg
     *            the synapse group being used to fill field values
     */
    public void fillFieldValues(final SynapseGroup sg) {
        if (sg.getId() == null || sg.getId().isEmpty()) {
            idField.setText(sg.getParentNetwork().getGroupIdGenerator()
                .getHypotheticalId()); // sg hasn't been added to the
                                       // network
                                       // yet
        } else {
            idField.setText(sg.getId());
        }
        if (sg.getLabel() == null || sg.getLabel().isEmpty()) {
            nameField.setText(idField.getText());
        } else {
            nameField.setText(sg.getLabel());
        }


        useGlobalSettings.setSelected(sg.isUseGroupLevelSettings());

        if (sg.size() > 0) {
            populationField.setText(Integer.toString(sg.size()));
            Iterator<Synapse> synIter;
            Synapse protoSyn;
            boolean discrepancy;
            // Excitatory field
            if (sg.getExcitatorySynapses().size() > 0) {
                synIter = sg.getExcitatorySynapses().iterator();
                protoSyn = synIter.next();
                discrepancy = false;
                while (synIter.hasNext()) {
                    if (!synIter.next().getLearningRule().getClass()
                        .equals(protoSyn.getLearningRule().getClass())) {
                        discrepancy = true;
                        break;
                    }
                }
                if (!discrepancy) {
                    excitatoryTypeField.setText(protoSyn.getLearningRule()
                        .getName());
                } else {
                    excitatoryTypeField.setText("Mixed");
                }
            } else {
                excitatoryTypeField.setText("None");
            }
            // Inhibitory Field
            if (sg.getInhibitorySynapses().size() > 0) {
                synIter = sg.getInhibitorySynapses().iterator();
                protoSyn = synIter.next();
                discrepancy = false;
                while (synIter.hasNext()) {
                    if (!synIter.next().getLearningRule().getClass()
                        .equals(protoSyn.getLearningRule().getClass())) {
                        discrepancy = true;
                        break;
                    }
                }
                if (!discrepancy) {
                    inhibitoryTypeField.setText(protoSyn.getLearningRule()
                        .getName());
                } else {
                    inhibitoryTypeField.setText("Mixed");
                }
            } else {
                inhibitoryTypeField.setText("None");
            }
        } else {
            populationLabel.setText("0");
            // Handles if an empty synapse group is selected
            excitatoryTypeField.setText("None");
            inhibitoryTypeField.setText("None");
        }
        parentGroupField.setText(sg.getParentGroup() == null ? "None" : sg
            .getParentGroup().getLabel());
        incomingField.setText(sg.getSourceNeuronGroup().getLabel());
        outgoingField.setText(sg.getTargetNeuronGroup().getLabel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean commitChanges() {
        group.setLabel(nameField.getText());

        if (group instanceof SynapseGroup) {
            ((SynapseGroup) group).setUseGroupLevelSettings(useGlobalSettings
                .isSelected());
        }

        if (group instanceof NeuronGroup) {
            try {
                ((NeuronGroup) group).setInputMode(inputMode.isSelected());
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(null, "Changes not saved!"
                        + "\nIssue: Input data is missing."
                        + "\nInput data can be set under the"
                        + " \"Input Data\" tab of this dialog.",
                        "Missing Input Data",
                        JOptionPane.WARNING_MESSAGE);
                inputMode.setSelected(false);
            }
        }

        return true; // Always Successful: the only field it makes sense to
        // commit from here cannot fail as a result of user action.
    }

    public JLabel getIdField() {
        return idField;
    }

    public void setIdField(JLabel idField) {
        this.idField = idField;
    }

    public JTextField getNameField() {
        return nameField;
    }

    public void setNameField(JTextField nameField) {
        this.nameField = nameField;
    }

    public JLabel getPopField() {
        return populationField;
    }

    public void setPopField(JLabel popField) {
        this.populationField = popField;
    }

    public JLabel getTypeField() {
        return excitatoryTypeField;
    }

    public void setTypeField(JLabel typeField) {
        this.excitatoryTypeField = typeField;
    }

    public JLabel getParentGroupField() {
        return parentGroupField;
    }

    public void setParentGroupField(JLabel parentGroupField) {
        this.parentGroupField = parentGroupField;
    }

    public JLabel getIncomingField() {
        return incomingField;
    }

    public void setIncomingField(JLabel incomingField) {
        this.incomingField = incomingField;
    }

    public JLabel getOutgoingField() {
        return outgoingField;
    }

    public void setOutgoingField(JLabel outgoingField) {
        this.outgoingField = outgoingField;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getPanel() {
        return this;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        initializeLayout();
        repaint();
    }

    public JTextField getEditablePopField() {
        return editablePopulationField;
    }

    public JLabel getPopLabel() {
        return populationLabel;
    }

    public JCheckBox getUseGlobalSettingsChkBx() {
    	return useGlobalSettings;
    }
    
    @Override
    public void fillFieldValues() {
        if (group instanceof SynapseGroup) {
            fillFieldValues((SynapseGroup) group);
        } else if (group instanceof NeuronGroup) {
            fillFieldValues((NeuronGroup) group);
        }
    }
    
}
