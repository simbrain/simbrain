/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.trainer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.trainers.IterableAlgorithm;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.network.trainers.TrainerListener;
import org.simbrain.util.ClassDescriptionPair;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.genericframe.GenericJFrame;

/**
 * GUI for supervised learning in Simbrain, using back-propagation, LMS, and
 * (eventually) other algorithms. A GUI front end for the trainer class.
 *
 * @author Jeff Yoshimi
 */
public class TrainerPanel extends JPanel {

    /** Data window. */
    private JPanel dataWindow;

    /** Reference to trainer object. */
    private Trainer trainer;

    /** Combo box for training algorithm. */
    private JComboBox cbTrainingAlgorithm;

    /** Top panel. */
    private JPanel topItems;

    /** Run panel; changes depending on learning rule selected. */
    private JPanel runPanel;

    /** Data panel. */
    private JPanel dataPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    /** Combo box for training algorithm. */
    private JComboBox cbDataFormat;

    /** Parent frame. */
    private GenericFrame parentFrame;

    /**
     * Type of the data viewer: an input data viewer or training data viewer.
     */
    enum DataFormat {
        LOAD_DATA {
            public String toString() {
                return "Load data";
            }
        },
        SINGLE_STEP {
            public String toString() {
                return "Use current activations";
            }
        };
    };

    /** Panel showing the current "run control" */
    private JPanel currentTrainerRunControls;

    /**
     * Type of the data viewer: an input data viewer or training data viewer.
     */
    public enum TrainerDataType {
        Input, Trainer
    };

    /** Reference to parent panel. Used as a reference for displaying the trainer panel. */
    private final NetworkPanel panel;
    
    /**
     * Construct a trainer panel with a specified set of rules.
     *
     * @param networkPanel the parent network panel
     * @param trainer the trainer this panel represents
     * @param ruleList the set of rules to display
     */
    public TrainerPanel(final NetworkPanel networkPanel, final Trainer trainer, ClassDescriptionPair[] ruleList) {
        // Initial setup
        this.trainer = trainer;
        this.panel = networkPanel;

        // Main Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Top items
        topItems = new JPanel();
        topItems.setLayout(new FlowLayout(FlowLayout.LEFT));
        topItems.setBorder(BorderFactory.createTitledBorder("Learning Rule"));
        JPanel comboBoxPlusButton = new JPanel();
        cbTrainingAlgorithm = new JComboBox(ruleList);  
        cbTrainingAlgorithm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                trainerChanged();
            }
        });
        if (ruleList.length == 1) {
            comboBoxPlusButton.add(new JLabel(ruleList[0].getSimpleName()));
        } else {
            comboBoxPlusButton.add(cbTrainingAlgorithm);            
        }
        JButton properties = new JButton(
                TrainerGuiActions.getPropertiesDialogAction(trainer));
        comboBoxPlusButton.add(properties);
        topItems.add(comboBoxPlusButton);
        mainPanel.add(topItems);

        // Center run panel
        runPanel = new JPanel();
        runPanel.setLayout(new BoxLayout(runPanel, BoxLayout.Y_AXIS));
        runPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        runPanel.setBorder(BorderFactory.createTitledBorder("Controls"));
        mainPanel.add("Center", runPanel);

        // Data windows
        dataWindow = new JPanel(new BorderLayout());
        dataWindow.setBorder(BorderFactory.createTitledBorder("Data"));
        dataWindow.setLayout(new BoxLayout(dataWindow, BoxLayout.Y_AXIS));
        cbDataFormat = new JComboBox(DataFormat.values());
        JPanel buffer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buffer.add(cbDataFormat);
        dataWindow.add("North", buffer);
        dataWindow.add(dataPanel);

        cbDataFormat.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                refreshDataPanel();
            }
        });

        mainPanel.add(dataWindow);

        // Add trainer listener
        initializeTrainerListener();

        // Initialize trainer specific panels
        refreshDataPanel();

        // Add mainPanel
        add(mainPanel);
    }
    
    /**
     * Construct a trainer panel around a trainer object.  Use the default set of rules.
     *
     * @param networkPanel the parent network panel
     * @param trainer the trainer this panel represents
     */
    public TrainerPanel(final NetworkPanel networkPanel, final Trainer trainer) {
        this(networkPanel, trainer, Trainer.getRuleList());
    }

    /**
     * Refresh the data panel.
     */
    private void refreshDataPanel() {
        dataPanel.removeAll();
        if (cbDataFormat.getSelectedItem() == DataFormat.LOAD_DATA) {
            JPanel loadPanel = new LoadDataPanel(getNetworkPanel(), trainer);
            dataPanel.add(loadPanel);
        } else if (cbDataFormat.getSelectedItem() == DataFormat.SINGLE_STEP) {
            JPanel singleStepPanel = new SingleStepPanel(trainer);
            dataPanel.add(singleStepPanel);
        }
        trainerChanged();
    }

    /**
     * Change the trainer based on the combo box selection.
     */
    private void trainerChanged() {
                
        // Update the training method
        String name = ((ClassDescriptionPair) cbTrainingAlgorithm
                .getSelectedItem()).getSimpleName();
        trainer.setTrainingMethod(name);
        
        if (cbDataFormat.getSelectedItem() != DataFormat.SINGLE_STEP) {
            trainer.getNetwork().clearActivations();
            trainer.getNetwork().clearBiases();
        }

        // Update graphics panel
        if (currentTrainerRunControls != null) {
            runPanel.remove(currentTrainerRunControls);
        }

        // Set run controls
        if (trainer.getTrainingMethod() instanceof IterableAlgorithm) {
            currentTrainerRunControls = new IterativeControlsPanel(getNetworkPanel(), trainer);
        } else {
            currentTrainerRunControls = createRunPanelNonIterable();
        }
        runPanel.add(currentTrainerRunControls);
        if (parentFrame != null) {
            parentFrame.pack();            
        }
    }

    /**
     * Initialize the trainer listener. Update the panel based on changes that
     * occur in the trainer.
     */
    private void initializeTrainerListener() {

        trainer.addListener(new TrainerListener() {

            /*
             * {@inheritDoc}
             */
            public void errorUpdated() {
                parentFrame.pack();
            }

            /*
             * {@inheritDoc}
             */
            public void inputDataChanged(double[][] inputData) {
                //System.out.println("Input Data Changed");
            }

            /*
             * {@inheritDoc}
             */
            public void trainingDataChanged(double[][] inputData) {
                //System.out.println("Training Data Changed");
            }

        });
    }

    /**
     * Create the "run" panel for non-iterable learning algorithms.
     *
     * @return the panel
     */
    private JPanel createRunPanelNonIterable() {
        JPanel runPanel = new JPanel();
        runPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 25, 5));
        JButton apply = new JButton("Apply");
        TrainerProgressBar tpb = new TrainerProgressBar(trainer, apply);
        runPanel.add(apply);
        runPanel.add(tpb);
        return runPanel;
    }


    /**
     * @return the trainer
     */
    final Trainer getTrainer() {
        return trainer;
    }

    /**
     * Test GUI.
     *
     * @param args
     */
    public static void main(String[] args) {
        RootNetwork network = new RootNetwork();
        int[] topology = new int[] { 2, 2, 1 };
        BackpropNetwork backprop = new BackpropNetwork(network, topology, null);
        network.addGroup(backprop);
        Trainer trainer = new Trainer(network, backprop.getInputLayer().getNeuronList(),
                backprop.getOutputLayer().getNeuronList(), "Backprop");
        GenericJFrame frame = new GenericJFrame();
        TrainerPanel trainerPanel = new TrainerPanel(new NetworkPanel(network), trainer);
        trainerPanel.setFrame(frame);
        frame.setContentPane(trainerPanel);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * @param parentFrame the parentFrame to set
     */
    public void setFrame(GenericFrame parentFrame) {
        this.parentFrame = parentFrame;
        parentFrame.setTitle("Train " + trainer.getTopologyDescription() + " net");          
    }

    /**
     * Return references to parent network panel.
     *
     * @return network panel.
     */
    public NetworkPanel getNetworkPanel() {
        return panel;
    }

}
