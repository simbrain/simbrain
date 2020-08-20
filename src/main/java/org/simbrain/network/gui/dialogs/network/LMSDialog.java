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

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.network.gui.trainer.DataPanel;
import org.simbrain.network.gui.trainer.IterativeControlsPanel;
import org.simbrain.network.subnetworks.LMSNetwork;
import org.simbrain.network.trainers.LMSIterative;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.table.NumericTable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */
public class LMSDialog extends StandardDialog {

    /**
     * Todo
     */
    private LMSNetwork lms;

    /**
     * Main tabbed pane.
     */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Reference to input data panel.
     */
    private DataPanel inputPanel;

    /**
     * Reference to training data panel.
     */
    private DataPanel trainingPanel;

    /**
     * Reference to validate inputs panel
     */
    private TestInputPanel validateInputsPanel;

    /**
     * List of tabs in the dialog.
     */
    private List<Component> tabs = new ArrayList<Component>();

    /**
     * Network panel.
     */
    protected NetworkPanel networkPanel;

    /**
     * Default constructor.
     *
     * @param np  parent panel
     * @param lms edited network
     */
    public LMSDialog(final NetworkPanel np, final LMSNetwork lms) {
        this.lms = lms;
        this.networkPanel = np;
        init();
        initDefaultTabs();
    }

    //TODO: Temp for testing
    INDArray inputs = Nd4j.eye(5);
    INDArray targets = inputs.dup();
    DataSet dataset = new DataSet(inputs, targets);
    MultiLayerNetwork net;
    {
        inputs = inputs.addRowVector(Nd4j.ones(1,5));
        targets = inputs.addRowVector(Nd4j.ones(1,5));
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        setTitle("Edit LMS Network");

        // Main vertical box
        Box trainerPanel = Box.createVerticalBox();

        // TODO (Ken): Put in GUI hooks to more DL4J things so that more parts of training can be customized
        // Feel free to have a look at DataPanel.

        JButton trainButton = new JButton("Train");
        trainerPanel.add(trainButton);
        trainButton.addActionListener(e -> {
            train();
        });
        trainerPanel.add(trainButton);

        // TODO: Init button? Where to put this?
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                // Using stochastic gradient decent
                .updater(new Sgd(0.2))
                .seed(1)
                .biasInit(0)
                .miniBatch(false)
                .list()
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MEAN_ABSOLUTE_ERROR)
                        .nIn(5)
                        .nOut(5)
                        .activation(Activation.RELU)
                        .weightInit(new UniformDistribution(0, 1))
                        .build())
                .build();

        net = new MultiLayerNetwork(config);
        net.init();

        // Add to tabbed pane
        addTab("Train", trainerPanel);

    }

    // TODO: This should be in LMSNetwork
    private void train() {
        for (int i = 0; i < 25; i++) {
            net.fit(dataset);
            System.out.println("score:" + net.score());
        }

    }

    /**
     * This method initializes the components on the panel.
     */
    protected void initDefaultTabs() {

        // Set to modeless so the dialog can be left open
        setModalityType(ModalityType.MODELESS);


        // Input data tab
        inputPanel = new DataPanel(inputs);
        inputPanel.setFrame(this);
        addTab("Input data", inputPanel);

        // Training data tab
        trainingPanel = new DataPanel(targets);
        trainingPanel.setFrame(this);
        addTab("Target data", trainingPanel);

        // Testing tab
        // validateInputsPanel = TestInputPanel.createTestInputPanel(networkPanel, lms.getInputNeurons(),
        //         lms.getTrainingSet().getInputDataMatrix());
        // addTab("Validate Input Data", validateInputsPanel);

        // Finalize
        setContentPane(tabbedPane);

    }

    /**
     * Add a tab to the dialog.
     *
     * @param name name to be displayed
     * @param tab  the tab itself
     */
    public void addTab(String name, Component tab) {
        if (tabs.size() == 0) {
            tabbedPane.addTab(name, tab);
        } else {
            tabbedPane.addTab(name, new JPanel());
        }
        tabs.add(tab);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        super.closeDialogOk();
        // inputPanel.commitChanges();
        // trainingPanel.commitChanges();
    }
}
