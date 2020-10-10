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
import org.simbrain.network.gui.trainer.ErrorPlotPanel;
import org.simbrain.network.subnetworks.LMSNetwork;
import org.simbrain.network.trainers.IterableTrainer;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.propertyeditor.EditableObject;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog to edit an {@link LMSNetwork}.
 */
public class LMSTrainingDialog extends StandardDialog {

    /**
     * The LMS Network being edited.
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
     * LMS Configuration object that is edited using an {@link AnnotatedPropertyEditor}
     */
    private LMSConfig lmsConfig = new LMSConfig();

    /**
     * Underlying DL4J Object.  After training, its weights and biases should be
     *    passed on to the LMSNetwork.
     */
    private MultiLayerNetwork mln;

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
    public LMSTrainingDialog(final NetworkPanel np, final LMSNetwork lms) {
        this.lms = lms;
        this.networkPanel = np;
        init();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        setTitle("Edit LMS Network");

        // Set to modeless so the dialog can be left open
        setModalityType(ModalityType.MODELESS);

        // Main vertical box
        Box trainerPanel = Box.createVerticalBox();

        // Time series for error.
        LMSTrainer trainer = new LMSTrainer();
        ErrorPlotPanel errorPanel = new ErrorPlotPanel(trainer);
        trainerPanel.add(errorPanel);

        // Button to initialize the network object
        JButton initButton = new JButton("Initialize network");
        initButton.addActionListener(e -> {
            AnnotatedPropertyEditor configPanel = new AnnotatedPropertyEditor(lmsConfig);
            StandardDialog dialog = configPanel.getDialog();
            dialog.makeVisible();
            dialog.getOkButton().setText("Init");
            dialog.addClosingTask(() -> {
                configPanel.commitChanges();
                initNetwork();
            });
        });
        trainerPanel.add(initButton);

        // Train the network
        JButton trainButton = new JButton("Train");
        trainerPanel.add(trainButton);
        trainButton.addActionListener(e -> {
            try {
                trainer.iterate2();
            } catch (IterableTrainer.DataNotInitializedException dataNotInitializedException) {
                dataNotInitializedException.printStackTrace();
            }
        });
        trainerPanel.add(trainButton);

        // Add to tabbed pane
        tabbedPane.addTab("Train", trainerPanel);

        // Input data tab
        inputPanel = new DataPanel(lms.getInputs());
        inputPanel.setFrame(this);
        tabbedPane.addTab("Input data", inputPanel);

        // Training data tab
        trainingPanel = new DataPanel(lms.getTargets());
        trainingPanel.setFrame(this);
        tabbedPane.addTab("Target data", trainingPanel);

        // Testing tab
        validateInputsPanel = new TestInputPanel(networkPanel, lms);
        tabbedPane.addTab("Validate Input Data", validateInputsPanel);

        // Finalize
        setContentPane(tabbedPane);

        // See SupervisedTrainingDialog.  Can use this to reset size of tabs.
        tabbedPane.addChangeListener(e -> {
            int index =  ((JTabbedPane) e.getSource()).getSelectedIndex();
            if (index == 0) {
                inputPanel.commitChanges();
                trainingPanel.commitChanges();
            }
        });

    }

    /**
     * Initialize the nd4j network using an edited configuration object.
     */
    private void initNetwork() {
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                // Using stochastic gradient decent
                .updater(new Sgd(lmsConfig.learningRate))
                .seed(lmsConfig.seed)
                .biasInit(lmsConfig.initalBias)
                .miniBatch(lmsConfig.useMiniBatch)
                .list()
                .layer(new OutputLayer.Builder(lmsConfig.lossFunc)
                        .nIn(lms.getNAList().get(0).getNumNodes())
                        .nOut(lms.getNAList().get(1).getNumNodes())
                        .activation(lmsConfig.actFunc)
                        .weightInit(new UniformDistribution(0, 1)) //TODO
                        .build())
                .build();

        // TODO: Use config file from LMSNetwork, and draw weights and biases from it as well
        mln = new MultiLayerNetwork(config);
        mln.init();
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        super.closeDialogOk();
    }

    private class LMSTrainer extends IterableTrainer {

        @Override
        public double getError() {
            return mln.score();
        }

        @Override
        public void randomize() {
            // mln.init();
        }

        @Override
        protected TrainingSet getTrainingSet() {
            // TODO
            return null;
        }

        @Override
        public void apply() throws DataNotInitializedException {
            lms.train(mln, lms.getDataset());
        }
    }
    private class LMSConfig implements EditableObject {

        @UserParameter(label = "Loss Function", order = 10)
        private LossFunctions.LossFunction lossFunc = LossFunctions.LossFunction.MSE;

        @UserParameter(label = "Activation Function", order = 20)
        private Activation actFunc = Activation.SIGMOID;

        @UserParameter(label = "Minibatch", order = 30)
        private boolean useMiniBatch = true;

        @UserParameter(label = "Learning Rate", minimumValue = 0, increment = .01, order = 40)
        private double learningRate = .2;

        @UserParameter(label = "Seed", minimumValue = 1, increment = 1, order = 50)
        private int seed = 1;

        @UserParameter(label = "Initial Bias", minimumValue = 0.0, increment = .1, order = 60)
        private double initalBias = 0.0;

        @Override
        public String getName() {
            return "Optimizer Settings";
        }

        // Somehow deal with DL4JInvalidConfigException here

    }


}
