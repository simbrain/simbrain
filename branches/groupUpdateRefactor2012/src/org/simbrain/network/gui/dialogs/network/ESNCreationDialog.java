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
package org.simbrain.network.gui.dialogs.network;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.simbrain.network.groups.subnetworks.EchoStateNetwork;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.interfaces.NeuronUpdateRule;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron.SigmoidType;
import org.simbrain.network.trainers.LMSOffline.SolutionType;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;

/**
 * Creates a GUI dialog for the creation of an arbitrary echo-state network.
 * @author ztosi
 */
@SuppressWarnings("serial")
public class ESNCreationDialog extends StandardDialog {

    /**Underlying network panel*/
    private final NetworkPanel panel;

    /**Dialog panel*/
    private LabelledItemPanel esnPanel = new LabelledItemPanel();

    /**Text field: reads in number of input units*/
    private JTextField tfNumInputs = new JTextField();

    /**Text field: reads in number of reservoir units*/
    private JTextField tfNumReservoir = new JTextField();

    /**Text field: reads in number of output units*/
    private JTextField tfNumOutputs = new JTextField();

    /**Text field: reads in desired max eigenvalue*/
    private JTextField maxEigenValue = new JTextField();

    /**Text field: reads in desired reservoir sparsity*/
    private JTextField resSparsity = new JTextField();

    /**Text field: reads in desired sparsity between the input and reservoir*/
    private JTextField inResSparsity = new JTextField();

    /**Text field: reads in the desired sparsity between the output and
     * reservoir*/
    private JTextField backSparsity = new JTextField();

    private JTextField noiseMax = new JTextField();
    
    private JTextField noiseMin = new JTextField();
    
    /** A check-box which determines whether or not this ESN will have recurrent
     * output weights*/
    private JCheckBox recurrentOutputWeights = new JCheckBox();

    /** A check-box which determines whether or not this ESN will have weights
     * from the output layer to the reservoir.
     */
    private JCheckBox backWeights = new JCheckBox();

    /** A check-box which destermines whether or not this ESN will have weights
     * directly from input to output*/
    private JCheckBox directInOutWeights = new JCheckBox();
    
    private JCheckBox noise = new JCheckBox();

    /**Maps string values to corresponding NeuronUpdateRules for the
     * combo-boxes governing desired Neuron type for a given layer
     */
    private HashMap<String, NeuronUpdateRule> boxMap =
        new HashMap<String, NeuronUpdateRule>();

    /**A button which brings up a file chooser for input data used for
     * training*/
    private JButton inputDataButton = new JButton();

    /**A button which brings up a file chooser for teacher data used for
     * training*/
    private JButton teacherDataButton = new JButton();

    /**Input data for training*/
    private double [][] inputData;

    /**Teacher data for training*/
    private double [][] teacherData;

    //Mapping of Strings to NeuronUpdateRules, currently only Logisitc, Tanh,
    //and Linear neurons are allowed.
    {
        boxMap.put("Linear", new LinearNeuron());
        SigmoidalNeuron sig0 = new SigmoidalNeuron();
        sig0.setType(SigmoidType.LOGISTIC);
        boxMap.put("Logistic", sig0);
        SigmoidalNeuron sig1 = new SigmoidalNeuron();
        sig1.setType(SigmoidType.TANH);
        boxMap.put("Tanh", sig1);
    }


    /** String values for combo-boxes (same as key values for boxMap)*/
    private String [] nTypeOptions = {"Linear", "Tanh", "Logistic"};

    /**Combo-box governing desired neuron type of the reservoir*/
    private JComboBox reservoirNeuronTypes = new JComboBox(nTypeOptions);

    /**Combo-box governing the desired neuron type of the output layer*/
    private JComboBox outputNeuronTypes = new JComboBox(nTypeOptions);

    /**Possible methods of linear regression for training*/
    private String [] regressionOptions = {"Weiner-Hopf", "Moore-Penrose"};

    /**Combo-box governing desired linear regression solution for training*/
    private JComboBox linearRegressionSol = new JComboBox(regressionOptions);

    /**
     * Creation dialog constructor.
     * @param panel
     *             Underlying network panel
     */
    public ESNCreationDialog(final NetworkPanel panel) {
        this.panel = panel;

        //For customized values
        GridBagConstraints gbc = new GridBagConstraints();

        setTitle("Build Echo-State Network ");
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        esnPanel.setMyNextItemRow(1);
        gbc.gridy = esnPanel.getMyNextItemRow();
        //Align to upper left
        gbc.anchor = GridBagConstraints.NORTHWEST;

        //Create section for network parameters
        sectionSeparator("Network Parameters", gbc, 1);

        //Add text-fields
        esnPanel.addItem("Input Nodes:", tfNumInputs);
        esnPanel.addItem("Reservoir Neuron Type:", reservoirNeuronTypes, 2);
        esnPanel.addItem("Reservoir Nodes:", tfNumReservoir);
        esnPanel.addItem("Output Neuron Type:", outputNeuronTypes, 2);
        esnPanel.addItem("Output Nodes:", tfNumOutputs);

        //GridBagConstraints for next section
        int row = esnPanel.getMyNextItemRow();
        row += 3;
        //Moves everything down
        esnPanel.setMyNextItemRow(row);
        gbc.gridx = 0;
        gbc.gridy = esnPanel.getMyNextItemRow();

        //Adds section for connectivity parameters
        sectionSeparator("Connectivity Parameters", gbc, row);

        //Add connectivity parameter check-boxes and text fields
        esnPanel.addItem("Input- reservoir connectivity:", inResSparsity, 2);
        esnPanel.addItem("Recurrent output weights:", recurrentOutputWeights);
        esnPanel.addItem("Reservoir connectivity: ", resSparsity, 2);
        esnPanel.addItem("Direct input to output weights:", directInOutWeights);
        esnPanel.addItem("Back weight connectivity: ", backSparsity, 2);
        //Default is disabled
        backSparsity.setEnabled(false);
        esnPanel.addItem("Back weights:", backWeights);
        row = esnPanel.getMyNextItemRow();
        esnPanel.setMyNextItemRow(row++);
        esnPanel.addItem("Spectral radius:", maxEigenValue, 2);

        //Creates action listener which enables the text field for back weight
        //sparsity based on if back weights are desired given the state of
        //its check-box
        backWeights.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (backWeights.isSelected()) {
                    backSparsity.setEnabled(true);
                } else {
                    backSparsity.setEnabled(false);
                }
            }
        });

        //Moves everything down
        esnPanel.setMyNextItemRow(row);
        gbc.gridx = 0;
        gbc.gridy = esnPanel.getMyNextItemRow();

        //sectionSeparator("Training Parameters", gbc, row);

        final JFileChooser fc = new JFileChooser();

        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "CSV files", "csv");
        fc.setFileFilter(filter);

        instantiateFileOpeners(fc);

        inputDataButton.setText("...");
        teacherDataButton.setText("...");

        noise.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                if (noise.isSelected()) {
                    noiseMax.setEnabled(true);
                    noiseMin.setEnabled(true);
                } else {
                    noiseMax.setEnabled(false);
                    noiseMin.setEnabled(false);
                }
            }

        }); {

        }

        //REDO
//        esnPanel.addItem("Regression Type: ", linearRegressionSol, 2);
//        esnPanel.addItem("Input Data: ", inputDataButton);
//        esnPanel.addItem("Maximum Noise: ", noiseMax, 2);
//        esnPanel.addItem("Teacher Data:", teacherDataButton);
//        esnPanel.addItem("Minimum Noise: ", noiseMin, 2);
//        esnPanel.addItem("Noise: ", noise);

        setContentPane(esnPanel);
        fillFieldValues();

    }

    /**
     * Instantiates the action listeners for the buttons responsible for
     * opening file-chooser dialog boxes
     * @param fc
     *          the file chooser dialog for the buttons
     */
    private void instantiateFileOpeners(final JFileChooser fc) {

        inputDataButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == inputDataButton) {
                    int returnVal = fc.showOpenDialog(new JPanel());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        setInputData(Utils.getDoubleMatrix(
                                fc.getSelectedFile()));
                    }
                }
            }
        });

        teacherDataButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == teacherDataButton) {
                    int returnVal = fc.showOpenDialog(new JPanel());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        setTeacherData(Utils.getDoubleMatrix(
                                fc.getSelectedFile()));
                    }
                }
            }
        });
    }

    //TODO: put this in a more public class?
    /**
     * Creates a new dialog section given a title and using a JSeparator.
     * @param label name of the section
     * @param gbc current GridBagConstraints, to align label and separators
     * @param cRow current row relative to LabeledItemPanel
     */
    public void sectionSeparator(String label, GridBagConstraints gbc,
            int cRow) {
        //Section label
        esnPanel.add(new JLabel(label), gbc);

        //Place separator directly below label
        cRow++;
        esnPanel.setMyNextItemRow(cRow);
        gbc.gridy = esnPanel.getMyNextItemRow();

        //Add separators incrementing grix each time to cover each column
        esnPanel.add(new JSeparator(JSeparator.HORIZONTAL), gbc);
        gbc.gridx = 1;
        esnPanel.add(new JSeparator(JSeparator.HORIZONTAL), gbc);
        gbc.gridx = 2;
        esnPanel.add(new JSeparator(JSeparator.HORIZONTAL), gbc);

        //Ensures section content will be below section separator
        cRow++;
        esnPanel.setMyNextItemRow(cRow);
        //Reset column value
        gbc.gridx = 0;
    }


    /**
     * Populate fields with default data.
     */
    public void fillFieldValues() {
        tfNumInputs.setText("" + 5);
        tfNumReservoir.setText("" + 25);
        tfNumOutputs.setText("" + 7);
        recurrentOutputWeights.setSelected(false);
        directInOutWeights.setSelected(false);
        backWeights.setSelected(false);
        resSparsity.setText("" + 0.1);
        inResSparsity.setText("" + 1.0);
        backSparsity.setText(" " + .5);
        maxEigenValue.setText("" + 0.98);
    }

    /* (non-Javadoc)
     * @see org.simbrain.util.StandardDialog#closeDialogOk()
     */
    @Override
    protected void closeDialogOk() {

        try{

            if (Integer.parseInt(tfNumReservoir.getText()) < 10) {
                JOptionPane.showMessageDialog(null,
                        "Too few reservoir neurons",
                        "Warning!", JOptionPane.WARNING_MESSAGE);
                return;
            }

            //Initialize logical network builder
            EchoStateNetwork builder = new EchoStateNetwork(
                    panel.getRootNetwork(),
                    //Get layer size values from fields...
                    Integer.parseInt(tfNumInputs.getText()),
                    Integer.parseInt(tfNumReservoir.getText()),
                    Integer.parseInt(tfNumOutputs.getText()),
                    panel.getLastClickedPosition());

            //Get connection parameters from fields
            builder.setInSparsity(Double.parseDouble(inResSparsity.getText()));
            builder.setResSparsity(Double.parseDouble(resSparsity.getText()));
            builder.setBackWeights(backWeights.isSelected());
            if (backWeights.isSelected()) {
                builder.setBackSparsity(Double.parseDouble(
                        backSparsity.getText()));
            }
            builder.setSpectralRadius(
                    Double.parseDouble(maxEigenValue.getText()));
            builder.setRecurrentOutWeights(
                    recurrentOutputWeights.isSelected());
            builder.setDirectInOutWeights(
                    directInOutWeights.isSelected());
            NeuronUpdateRule resUp =
                boxMap.get(reservoirNeuronTypes.getSelectedItem());
            builder.setReservoirNeuronType(resUp);
            NeuronUpdateRule outUp =
                boxMap.get(outputNeuronTypes.getSelectedItem());
            builder.setOutputNeuronType(outUp);

            if (linearRegressionSol.getSelectedItem() == "Weiner-Hopf") {
                builder.setSolType(SolutionType.WIENER_HOPF);
            } else {
                builder.setSolType(SolutionType.MOORE_PENROSE);
            }

            if (noise.isSelected()) {
                builder.setNoise(true);
                builder.setNoiseMax(Double.parseDouble(noiseMax.getText()));
                builder.setNoiseMin(Double.parseDouble(noiseMin.getText()));
            }

            //Build network
            builder.buildNetwork();

            dispose();

            //REDO
//            int dialogOpts = JOptionPane.showConfirmDialog(new JPanel(),
//                    "Network Construction Complete. \nBegin training "
//                    + "procedure?\n(Warning: This may a while)\n");
//
//            if (dialogOpts == JOptionPane.YES_OPTION) {
//                builder.train(inputData, teacherData);
//                esnPanel.setVisible(false);
//                dispose();
//                panel.repaint();
//            } else if (dialogOpts == JOptionPane.NO_OPTION) {
//                panel.clearPanel();
//            } else {
//                panel.clearPanel();
//                esnPanel.setVisible(false);
//                dispose();
//            }


        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null,
                    "Inappropriate Field Values:"
                    + "\nNetwork construction failed.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

    }

    public void setInputButton(JButton inputData) {
        this.inputDataButton = inputData;
    }

    public JButton getInputButton() {
        return inputDataButton;
    }

    public void setTeacherButton(JButton teacherData) {
        this.teacherDataButton = teacherData;
    }

    public JButton getTeacherButton() {
        return teacherDataButton;
    }

    public void setInputData(double [][] inputData) {
        this.inputData = inputData;
    }

    public double [][] getInputData() {
        return inputData;
    }

    public void setTeacherData(double [][] teacherData) {
        this.teacherData = teacherData;
    }

    public double [][] getTeacherData() {
        return teacherData;
    }

}
