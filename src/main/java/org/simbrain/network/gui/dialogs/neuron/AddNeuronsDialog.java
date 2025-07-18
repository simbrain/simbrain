package org.simbrain.network.gui.dialogs.neuron;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.updaterules.LinearRule;
import org.simbrain.network.updaterules.NeuronUpdateRule;
import org.simbrain.util.DetailTrianglePanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A dialog for adding multiple neurons to the network. User can specify a
 * neuron type and a layout.
 *
 * @author ztosi
 * @author jyoshimi
 */
public class AddNeuronsDialog extends StandardDialog {

    /**
     * The default layout.
     */
    private static final Layout DEFAULT_LAYOUT = new GridLayout();

    /**
     * The default neuron.
     */
    private static final NeuronUpdateRule DEFAULT_NEURON = new LinearRule();

    /**
     * Default number of neurons.
     */
    private static final int DEFAULT_NUM_NEURONS = 100;

    /**
     * The network panel neurons will be added to.
     */
    private final NetworkPanel networkPanel;

    /**
     * The base neuron to copy.
     */
    private final Neuron baseNeuron;

    /**
     * Help Button. Links to information about the currently selected neuron
     * update rule.
     */
    private final JButton helpButton = new JButton("Help");

    /**
     * Item panel where options will be displayed.
     */
    private final Box addNeuronsPanel = Box.createVerticalBox();

    /**
     * Text field where desired number of neurons is entered.
     */
    private final JTextField numNeurons = new JTextField("" + DEFAULT_NUM_NEURONS);

    /**
     * The panel containing basic information on the neurons as well as options
     * for setting their update rule and its parameters.
     */
    private AnnotatedPropertyEditor combinedNeuronInfoPanel;

    /**
     * Layout object.
     */
    private final Layout.LayoutEditor layoutObject = new Layout.LayoutEditor();

    /**
     * A panel where layout settings can be edited.
     */
    private AnnotatedPropertyEditor selectLayout;

    /**
     * A panel for editing whether or not the neurons will be added as a group.
     */
    private NeuronGroupPanelLite groupPanel;

    /**
     * A List of the neurons being added to the network.
     */
    private final List<Neuron> addedNeurons = new ArrayList<Neuron>();

    /**
     * A factory method that creates an AddNeuronsDialog to prevent references
     * to "this" from escaping during construction.
     *
     * @param networkPanel the network panel neurons will be added to.
     * @return an AddNeuronsDialog
     */
    public static AddNeuronsDialog createAddNeuronsDialog(final NetworkPanel networkPanel) {
        final AddNeuronsDialog addND = new AddNeuronsDialog(networkPanel);
        addND.combinedNeuronInfoPanel = new AnnotatedPropertyEditor<>(Collections.singletonList(addND.baseNeuron));
        addND.init();
        return addND;
    }

    /**
     * Constructs the dialog.
     *
     * @param networkPanel the panel the neurons are being added to.
     */
    private AddNeuronsDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        baseNeuron = new Neuron();
        networkPanel.getSelectionManager().clear();
    }

    /**
     * Initializes the add neurons panel with default settings.
     */
    private void init() {

        setTitle("Add Neurons...");

        // Basics Sub-Panel
        JPanel basicsPanel = new JPanel(new GridBagLayout());
        basicsPanel.setBorder(BorderFactory.createTitledBorder("Quantity"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.8;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        basicsPanel.add(new JLabel("Number of Neurons:"), gbc);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(5, 3, 0, 0);
        gbc.weightx = 0.2;
        gbc.gridx = 1;
        basicsPanel.add(numNeurons, gbc);
        addNeuronsPanel.add(basicsPanel);

        // Neuron Properties Panel
        var combinedNeuronInfoDetailPanel = new DetailTrianglePanel(combinedNeuronInfoPanel, false);
        combinedNeuronInfoDetailPanel.setBorder(BorderFactory.createTitledBorder("Neuron Type"));
        addNeuronsPanel.add(combinedNeuronInfoDetailPanel);

        // Layout Panel
        selectLayout = new AnnotatedPropertyEditor<>(layoutObject);
        var selectLayoutDetailPanel = new DetailTrianglePanel(selectLayout, false);
        selectLayoutDetailPanel.setBorder(BorderFactory.createTitledBorder("Neuron Layout"));
        addNeuronsPanel.add(selectLayoutDetailPanel);

        // Group Panel
        groupPanel = new NeuronGroupPanelLite(networkPanel);
        addNeuronsPanel.add(groupPanel);

        // Final setup
        setContentPane(new JScrollPane(addNeuronsPanel));
        this.addButton(helpButton);
    }

    /**
     * Adds the neurons to the panel.
     *
     * @param inGroup if true, add them in a group.
     */
    private void addNeurons(boolean inGroup) {
        int number = Integer.parseInt(numNeurons.getText());
            if (inGroup) {
                networkPanel.addNeuronGroupAsync(number, baseNeuron, layoutObject.getLayout(), groupPanel.tfGroupName.getText());
            } else {
                networkPanel.addNeuronsAsync(number, baseNeuron, layoutObject.getLayout());
            }
    }


    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        combinedNeuronInfoPanel.commitChanges();
        selectLayout.commitChanges();
        addNeurons(groupPanel.getAddToGroup().isSelected());
        dispose();
    }

    @Override
    protected void closeDialogCancel() {
        super.closeDialogCancel();
        dispose();
    }

    /**
     * A sub-panel which allows a user to put newly created neurons into a
     * neuron group. Options include a new neuron group, already existing neuron
     * group, or no neuron group (loose). The user can also change a group's
     * name from here.
     *
     * @author ztosi
     */
    @SuppressWarnings("serial")
    private class NeuronGroupPanelLite extends JPanel {

        /**
         * Select whether or not to add the neurons in a neuron group.
         */
        private final JCheckBox addToGroup = new JCheckBox();

        /**
         * A label for the neuron group name.
         */
        private final JLabel tfNameLabel = new JLabel("Name: ");

        /**
         * A text box for naming a new neuron group or renaming an existing one.
         */
        private final JTextField tfGroupName = new JTextField();

        /**
         * Creates the neuron group sub-panel
         *
         * @param np a reference to the network panel.
         */
        public NeuronGroupPanelLite(NetworkPanel np) {
            addListeners();
            setLayout(new BorderLayout());
            addToGroup.setSelected(false);

            JPanel groupPanel = new JPanel();
            groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.X_AXIS));
            groupPanel.add(addToGroup);
            groupPanel.add(Box.createHorizontalStrut(20));
            groupPanel.add(tfNameLabel);
            tfGroupName.setEnabled(addToGroup.isSelected());
            groupPanel.add(tfGroupName);
            groupPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            this.add(groupPanel, BorderLayout.CENTER);
            setBorder(BorderFactory.createTitledBorder("Group"));

        }

        /**
         * Adds (internal) listeners to the panel.
         */
        private void addListeners() {
            addToGroup.addActionListener(evt -> {
                tfGroupName.setEnabled(addToGroup.isSelected());
            });
        }

        public JCheckBox getAddToGroup() {
            return addToGroup;
        }

    }

}
