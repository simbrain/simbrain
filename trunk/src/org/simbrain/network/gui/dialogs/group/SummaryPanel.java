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

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.util.widgets.CommittablePanel;

/**
 * A panel which provides a high level summary of certain groups in Simbrain.
 * As of right now, Subnetworks are accepted, but only NeuronGroups and
 * SynapseGroups actually have implementations in this class.
 * TODO: Either implement or abandon subnetworks: Discussion Required
 *
 * @author ztosi
 */
@SuppressWarnings("serial")
public class SummaryPanel extends JPanel implements CommittablePanel {

    /** The default name for this type of panel. */
    public static final String DEFAULT_PANEL_NAME = "Summary";

    /** The default vertical gap between components of this panel. */
    private static final int DEFAULT_VGAP = 5; //px

    /** The default horizontal gap between components of this panel. */
    private static final int DEFAULT_HGAP = 50; //px

    /** The name of the panel. */
    private String name = DEFAULT_PANEL_NAME;

    /** The group this panel is summarizing. */
    private Group group;

    /** A flag for whether or not the population is editable from this panel.*/
    private boolean editable;

    /*/****************************************************
     *                   Field Labels                     *
     ******************************************************/

    /** Id label */
    private final JLabel idLabel = new JLabel("Group ID: ");

    /** Label label */
    private final JLabel nameLabel = new JLabel("Label: ");

    /** Population label */
    private final JLabel popLabel = new JLabel("Population: ");

    /** Type label */
    private final JLabel typeLabel = new JLabel("Type: ");

    /** A label for the parent group. */
    private final JLabel parentGroupLabel = new JLabel("Parent: ");

    /** A label for any incoming groups. */
    private final JLabel incomingGroupLabel = new JLabel();

    /** A label for any outgoing groups. */
    private final JLabel outgoingGroupLabel = new JLabel();


    /*/****************************************************
     *                      Fields                        *
     ******************************************************/

    /** The field for the group id. */
    private JLabel idField = new JLabel();

    /** The field for the name/label of the group. */
    private JTextField nameField = new JTextField();

    /** The population of the group field. */
    private JLabel popField = new JLabel();

    /**
     * An editable population field for situations (creation) where population
     * can be edited.
     */
    private JTextField editablePopField = new JTextField();

    /** The type of objects contained in the group field. */
    private JLabel typeField = new JLabel();

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

    /**
     * Constructs the summary panel based on a neuron group. Names the
     * incoming and outgoing labels appropriately (Incoming/Outgoing).
     * @param ng the neuron group for which a summary will be built
     * @param editable is this summary panel's population field editable? This
     * equates to asking if this summary panel is being used for creating (true)
     * or editing a neuron group (false).
     */
    public SummaryPanel(final NeuronGroup ng, boolean editable) {
        setGroup(ng);
        this.editable = editable;
        incomingGroupLabel.setText("Incoming: ");
        outgoingGroupLabel.setText("Outgoing: ");
        fillFieldValues(ng);
        initializeLayout();
    }

    /**
     * Constructs the summary panel based on a synapse group. Names the
     * incoming and outgoing labels appropriately (Source Group/Target Group).
     * @param sg the synapse group for which a summary will be built
     */
    public SummaryPanel(final SynapseGroup sg) {
        setGroup(sg);
        incomingGroupLabel.setText("Source Group: ");
        outgoingGroupLabel.setText("Target Group: ");
        fillFieldValues(sg);
        initializeLayout();
    }

    /**
     * TODO: Include? Discuss...
     * @param subNet the subnetwork for which a summary will be built
     */
    public SummaryPanel(Subnetwork subNet) {
        fillFieldValues(subNet);
    }

    /**
     *
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
        this.add(popLabel);
        if (isEditable()) {
            this.add(editablePopField);
        } else {
            this.add(popField);
        }
        this.add(typeLabel);
        this.add(typeField);
        if (!isEditable()) {
            this.add(parentGroupLabel);
            this.add(parentGroupField);
            this.add(incomingGroupLabel);
            this.add(incomingField);
            this.add(outgoingGroupLabel);
            this.add(outgoingField);
        }
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    /**
     *
     * @param ng the neuron group being used to fill the values of this panel's
     * fields.
     */
    private void fillFieldValues(final NeuronGroup ng) {
        if (ng.getId() == null || ng.getId().isEmpty()) {
            idField.setText(ng.getParentNetwork().getGroupIdGenerator()
                    .getHypotheticalId()); //ng hasn't been added to the network
                                           //yet
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
                editablePopField.setText(Integer
                        .toString(ng.getNeuronList().size()));
            } else {
                popField.setText(Integer.toString(ng.getNeuronList().size()));
            }
            if (NetworkUtils.isConsistent(ng.getNeuronList(), Neuron.class,
                    "getUpdateRuleDescription"))
            {
                typeField.setText(ng.getNeuronList().get(0).getUpdateRule()
                        .getDescription());
            } else {
                typeField.setText("Mixed");
            }
        } else {
            if (isEditable()) {
                // Is a creation dialog
                editablePopField.setText(Integer
                        .toString(NeuronGroup.DEFAULT_GROUP_SIZE));
                typeField.setText(Neuron.DEFAULT_UPDATE_RULE.getDescription());
            } else {
                // Handles if an empty neuron group is selected
                // (for some reason)...
                popField.setText(Integer.toString(0));
                typeField.setText("None");
            }
        }
        parentGroupField.setText(ng.getParentGroup() == null ? "None"
                : ng.getParentGroup().getLabel());
//        if (ng.getIncomingSg() == null) {
//            if (ng.getIncomingWeights() != null
//                    && ng.getIncomingWeights().size() > 0)
//            {
//                incomingField.setText("Loose (un-grouped) synapses");
//            } else {
//                incomingField.setText("None");
//            }
//        } else {
//            incomingField.setText(ng.getIncomingSg().getLabel());
//        }
//        if (ng.getOutgoingSg() == null) {
//            if (ng.getOutgoingWeights() != null
//                    && ng.getOutgoingWeights().size() > 0)
//            {
//                outgoingField.setText("Loose (un-grouped) synapses");
//            } else {
//                outgoingField.setText("None");
//            }
//        } else {
//            outgoingField.setText(ng.getIncomingSg().getLabel());
//        }
    }

    /**
     *
     * @param sg the synapse group being used to fill field values
     */
    private void fillFieldValues(final SynapseGroup sg) {
        if (sg.getId() == null || sg.getId().isEmpty()) {
            idField.setText(sg.getParentNetwork().getGroupIdGenerator()
                    .getHypotheticalId()); //sg hasn't been added to the network
                                           //yet
        } else {
            idField.setText(sg.getId());
        }
        if (sg.getLabel() == null || sg.getLabel().isEmpty()) {
            nameField.setText(idField.getText());
        } else {
            nameField.setText(sg.getLabel());
        }
        popField.setText(Integer.toString(sg.getSynapseList().size()));
        if (sg.getSynapseList().size() > 0) {
            if (NetworkUtils.isConsistent(sg.getSynapseList(), Synapse.class,
                    "getLearningRule"))
            {

                typeField.setText(sg.getSynapseList().get(0).getLearningRule()
                        .getDescription());

            } else {
                typeField.setText("Mixed");
            }
        } else {
            // Handles if an empty synapse group is selected
            typeField.setText("None");
        }
        parentGroupField.setText(sg.getParentGroup() == null ? "None"
                : sg.getParentGroup().getLabel());
        incomingField.setText(sg.getSourceNeuronGroup().getLabel());
        outgoingField.setText(sg.getTargetNeuronGroup().getLabel());
    }

    /**
     * TODO: include?
     * @param subNet the subnetwork being used to fill field values
     */
    private void fillFieldValues(Subnetwork subNet) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean commitChanges() {
        group.setLabel(nameField.getText());
        return true; // Always Successful: the only field it makes sense to
                     // commit from here cannot fail as a result of user action.
    }

    /*CHECKSTYLE:OFF***************************************
     *               Getters and Setters                  *
     ******************************************************/

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
        return popField;
    }

    public void setPopField(JLabel popField) {
        this.popField = popField;
    }

    public JLabel getTypeField() {
        return typeField;
    }

    public void setTypeField(JLabel typeField) {
        this.typeField = typeField;
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
        return editablePopField;
    }

    public void setEditablePopField(JTextField editablePopField) {
        this.editablePopField = editablePopField;
    }

    public JLabel getIdLabel() {
        return idLabel;
    }

    public JLabel getNameLabel() {
        return nameLabel;
    }

    public JLabel getPopLabel() {
        return popLabel;
    }

    public JLabel getTypeLabel() {
        return typeLabel;
    }

    public JLabel getParentGroupLabel() {
        return parentGroupLabel;
    }

    public JLabel getIncomingGroupLabel() {
        return incomingGroupLabel;
    }

    public JLabel getOutgoingGroupLabel() {
        return outgoingGroupLabel;
    }

    /*/****************************************************
     *              END Getters and Setters               *
     ****************************************CHECKSTYLE:ON*/

//    public static void main(String[] args) {
//        Network net = new Network();
//        Neuron n = new Neuron(net, new LinearRule());
//        NeuronGroup ng = new NeuronGroup(net, Collections.singletonList(n));
//
//        SummaryPanel sumP = new SummaryPanel(ng);
//
//        JFrame frame = new JFrame();
//
//        frame.setContentPane(sumP);
//
//        frame.pack();
//
//        frame.setVisible(true);
//    }

}
