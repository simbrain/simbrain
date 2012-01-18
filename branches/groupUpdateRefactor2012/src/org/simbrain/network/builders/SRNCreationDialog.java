package org.simbrain.network.builders;

import java.awt.GridBagConstraints;
import java.util.HashMap;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.simbrain.network.groups.SimpleRecurrentNetwork;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.interfaces.NeuronUpdateRule;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron.SigmoidType;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * Creates a GUI dialog to set the parameters for and then build a simple
 * recurrent network.
 * @author ztosi
 */

@SuppressWarnings("serial")
public class SRNCreationDialog extends StandardDialog {

    /** Underlying Network Panel */
    private final NetworkPanel panel;

    /** Underlying labeled item panel for dialog */
    private LabelledItemPanel srnPanel = new LabelledItemPanel();

    /** Text field for number of input nodes */
    private JTextField tfNumInputs = new JTextField();

    /** Text field for number of hidden layer nodes */
    private JTextField tfNumHidden = new JTextField();

    /** Text field for number of output nodes */
    private JTextField tfNumOutputs = new JTextField();

    /**
     * Maps string values to corresponding NeuronUpdateRules for the combo-boxes
     * governing desired Neuron type for a given layer*/
    private HashMap<String, NeuronUpdateRule> boxMap =
           new HashMap<String, NeuronUpdateRule>();

    /**
     * Mapping of Strings to NeuronUpdateRules, currently only Logisitc, Tanh,
     * and Linear neurons are allowed.
     */
    {
        boxMap.put("Linear", new LinearNeuron());
        SigmoidalNeuron sig0 = new SigmoidalNeuron();
        sig0.setType(SigmoidType.LOGISTIC);
        boxMap.put("Logistic", sig0);
        SigmoidalNeuron sig1 = new SigmoidalNeuron();
        sig1.setType(SigmoidType.TANH);
        boxMap.put("Tanh", sig1);
    }

    /** String values for combo-boxes (same as key values for boxMap) */
    private String[] options = { "Linear", "Tanh", "Logistic"};

    /** Combo box for selecting update rule for the hidden layer */
    private JComboBox hiddenNeuronTypes = new JComboBox(options);

    /** Combo box for selecting the update rule for the ourput layer */
    private JComboBox outputNeuronTypes = new JComboBox(options);

    /**
     * Constructs a labeled item panel dialog for the creation of a simple
     * recurrent network.
     * @param panel
     *            the network panel the SRN will be tied to
     */
    public SRNCreationDialog(final NetworkPanel panel) {
        this.panel = panel;

        // Grid bag constraints for manual positioning see #sectionSeparator
        GridBagConstraints gbc = new GridBagConstraints();

        setTitle("Build Simple Recurrent Network");

        // Set grid bag constraints
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        srnPanel.setMyNextItemRow(1);
        gbc.gridy = srnPanel.getMyNextItemRow();
        gbc.gridx = 0;

        // Add separator and title for fields below the separator
        sectionSeparator("Network Parameters", gbc, 1);

        // Add fields
        srnPanel.addItem("Number of input nodes:", tfNumInputs);
        srnPanel.addItem("Hidden Neuron Type:", hiddenNeuronTypes, 2);
        srnPanel.addItem("Number of Hidden nodes:", tfNumHidden);
        srnPanel.addItem("Output Neuron Type:", outputNeuronTypes, 2);
        srnPanel.addItem("Number of output nodes:", tfNumOutputs);

        // Fill fields with default values
        fillFieldValues();

        setContentPane(srnPanel);
    }

    /**
     * Creates a new dialog section given a title and using a JSeparator.
     * @param label
     *            name of the section
     * @param gbc
     *            current GridBagConstraints, to align label and separators
     * @param cRow
     *            current row relative to LabeledItemPanel
     */
    public void sectionSeparator(String label, GridBagConstraints gbc,
            int cRow) {
        // Section label
        srnPanel.add(new JLabel(label), gbc);

        // Place separator directly below label
        cRow++;
        srnPanel.setMyNextItemRow(cRow);
        gbc.gridy = srnPanel.getMyNextItemRow();

        // Add separators upping gridx each time to cover each column
        srnPanel.add(new JSeparator(JSeparator.HORIZONTAL), gbc);
        gbc.gridx = 1;
        srnPanel.add(new JSeparator(JSeparator.HORIZONTAL), gbc);
        gbc.gridx = 2;
        srnPanel.add(new JSeparator(JSeparator.HORIZONTAL), gbc);

        // Ensures section content will be below section separator
        cRow++;
        srnPanel.setMyNextItemRow(cRow);
        // Reset column value
        gbc.gridx = 0;
    }

    /**
     * Fills the fields with default values.
     */
    public void fillFieldValues() {
        tfNumInputs.setText("" + 12);
        tfNumHidden.setText("" + 10);
        tfNumOutputs.setText("" + 12);
    }

    @Override
    public void closeDialogOk() {
        try {

            SimpleRecurrentNetwork srnBuild = new SimpleRecurrentNetwork(panel.getRootNetwork(),
                    Integer.parseInt(tfNumInputs.getText()), Integer
                            .parseInt(tfNumHidden.getText()), Integer
                            .parseInt(tfNumOutputs.getText()),
                            panel.getLastClickedPosition());

            NeuronUpdateRule nur0 = boxMap.get(hiddenNeuronTypes
                    .getSelectedItem());
            srnBuild.setHiddenNeuronType(nur0);

            NeuronUpdateRule nur1 = boxMap.get(outputNeuronTypes
                    .getSelectedItem());
            srnBuild.setOutputNeuronType(nur1);

            srnBuild.build();
            srnPanel.setVisible(false);
            dispose();

        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Inappropriate Field Values:"
                    + "\nNetwork construction failed.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        panel.repaint();
    }

}
