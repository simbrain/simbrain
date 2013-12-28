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

import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.network.gui.trainer.DataPanel;
import org.simbrain.network.subnetworks.SOMNetwork;
import org.simbrain.network.trainers.SOMTrainer;
import org.simbrain.network.trainers.Trainer.DataNotInitializedException;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.table.NumericTable;

/**
 * Dialog for training a SOM network.
 *
 * @author Jeff Yoshimi
 *
 */
public class SOMTrainingDialog extends StandardDialog {

    /** Main tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Panel for setting properties of the SOM network. */
    private SOMPropertiesPanel somPropsPanel;

    /** Reference to network panel. */
    private NetworkPanel panel;

    /** Reference to the SOM Network. */
    private SOMNetwork network;

    /**
     * Construct the dialog.
     *
     * @param np parent network panel
     * @param network the SOM network
     */
    public SOMTrainingDialog(NetworkPanel np, SOMNetwork network) {

        this.panel = np;
        this.network = network;

        // Set to modeless so the dialog can be left open
        setModalityType(ModalityType.MODELESS);

        // Set up properties tab
        somPropsPanel = new SOMPropertiesPanel(np, network.getSom());
        tabbedPane.addTab("Network Properties", somPropsPanel);

        // Set up training tab
        SOMTrainerControlsPanel controlPanel = new SOMTrainerControlsPanel(
                new SOMTrainer(network));
        tabbedPane.addTab("Train Network", controlPanel);

        // Input data tab
        final DataPanel inputPanel = new DataPanel(network.getInputNeurons(),
                network.getTrainingSet().getInputDataMatrix(), 5, "Input");
        inputPanel.setFrame(this);
        tabbedPane.addTab("Training data", inputPanel);

        // Testing tab
        final TestInputPanel testInputPanel = new TestInputPanel(np,
                network.getInputNeurons(), network.getTrainingSet()
                        .getInputData());
        tabbedPane.addTab("Test data", testInputPanel);

        // Listen for tab changed events. Load inputs to test tab
        // If inputs have been loaded
        ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent
                        .getSource();
                int index = sourceTabbedPane.getSelectedIndex();
                // Just clicked out of Properties tab
                if (index == 2) {
                    somPropsPanel.commitChanges();
                }
                // Just clicked out of input tab
                if (index == 3) {
                    if (inputPanel.getTable().getData() != null) {
                        testInputPanel.setData(((NumericTable) inputPanel
                                .getTable().getData()).asDoubleArray());
                    }
                }
            }
        };
        tabbedPane.addChangeListener(changeListener);

        // Set up help
        Action helpAction = new ShowHelpAction(
                "Pages/Network/network/SOMnetwork.html");
        addButton(new JButton(helpAction));

        // Finish configuration
        setContentPane(tabbedPane);

    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        somPropsPanel.commitChanges();
    }

    /**
     * Training panel for SOM Network
     */
    private class SOMTrainerControlsPanel extends JPanel {

        /** Reference to trainer. */
        private SOMTrainer trainer;

        /** Current number of iterations. */
        private JLabel iterationsLabel = new JLabel("--- ");

        /** Current Learning Rate. */
        private JLabel lLearningRate = new JLabel();

        /** Current Neighborhood Size. */
        private JLabel lNeighborhoodSize = new JLabel();

        /**
         * Construct the SOM Training Controls Panel.
         *
         * @param trainer reference to the SOM trainer
         */
        public SOMTrainerControlsPanel(final SOMTrainer trainer) {

            this.trainer = trainer;
            init();
        }

        /**
         * Initialize the panel.
         */
        public void init() {
            // Set up properties tab
            Box propsBox = Box.createVerticalBox();
            propsBox.setOpaque(true);
            propsBox.add(Box.createVerticalGlue());

            // Run Tools
            JPanel runTools = new JPanel();
            runTools.add(new JLabel("Iterate: "));
            runTools.add(new JButton(runAction));
            JButton stepButton = new JButton(stepAction);
            stepButton.setHideActionText(true);
            runTools.add(stepButton);
            JButton resetButton = new JButton(resetAction);
            resetButton.setHideActionText(true);
            runTools.add(resetButton);
            JButton randomizeButton = new JButton(randomizeAction);
            randomizeButton.setHideActionText(true);
            runTools.add(randomizeButton);
            propsBox.add(runTools);

            // Separator
            JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
            propsBox.add(separator);

            // Properties
            Box lrBox = Box.createHorizontalBox();
            lrBox.add(new JLabel("Learning Rate:"));
            lrBox.add(Box.createHorizontalStrut(10));
            lrBox.add(lLearningRate);
            propsBox.add(lrBox);
            Box nbBox = Box.createHorizontalBox();
            nbBox.add(new JLabel("Neighborhood Size:"));
            nbBox.add(Box.createHorizontalStrut(10));
            nbBox.add(lNeighborhoodSize);
            propsBox.add(nbBox);

            // Separator
            JSeparator separator2 = new JSeparator(SwingConstants.HORIZONTAL);
            propsBox.add(separator2);

            // Labels
            LabelledItemPanel labelPanel = new LabelledItemPanel();
            labelPanel.addItem("Iterations:", iterationsLabel);
            propsBox.add(labelPanel);

            // Wrap it up
            add(propsBox);
            update();

        }

        /**
         * Update internal labels.
         */
        private void update() {
            lLearningRate.setText("" + network.getSom().getAlpha());
            lNeighborhoodSize.setText(""
                    + network.getSom().getNeighborhoodSize());
            iterationsLabel.setText("" + trainer.getIteration());
        }

        /**
         * A "play" action, that can be used to repeatedly iterate iterable
         * training algorithms.
         *
         */
        private Action runAction = new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Play.png"));
                // putValue(NAME, "Open (.csv)");
                putValue(SHORT_DESCRIPTION,
                        "Iterate training until stopping condition met");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                if (trainer == null) {
                    return;
                }
                if (trainer.isUpdateCompleted()) {
                    // Start running
                    trainer.setUpdateCompleted(false);
                    putValue(SMALL_ICON,
                            ResourceManager.getImageIcon("Stop.png"));
                    Executors.newSingleThreadExecutor().submit(new Runnable() {
                        public void run() {
                            try {
                                while (!trainer.isUpdateCompleted()) {
                                    trainer.apply();
                                    update();
                                    // if (showUpdates.isSelected()) {
//                                    panel.getNetwork()
//                                            .setUpdateCompleted(false);
//                                    panel.getNetwork().fireNetworkChanged();
//                                    while (!panel.getNetwork()
//                                            .isUpdateCompleted()) {
//                                        try {
//                                            Thread.sleep(1);
//                                        } catch (InterruptedException e) {
//                                            e.printStackTrace();
//                                        }
//                                    }
                                    // }
                                }
                            } catch (DataNotInitializedException e) {
                                JOptionPane.showOptionDialog(null,
                                        e.getMessage(), "Warning",
                                        JOptionPane.DEFAULT_OPTION,
                                        JOptionPane.WARNING_MESSAGE, null,
                                        null, null);
                            }
                        }
                    });
                } else {
                    // Stop running
                    trainer.setUpdateCompleted(true);
                    panel.getNetwork().fireNetworkChanged();
                    putValue(SMALL_ICON,
                            ResourceManager.getImageIcon("Play.png"));
                }

            }

        };

        /**
         * /** Apply training algorithm.
         */
        private Action stepAction = new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Step.png"));
                putValue(NAME, "Train network");
                // putValue(SHORT_DESCRIPTION, "Import table from .csv");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                if (trainer == null) {
                    return;
                }
                try {
                    trainer.apply();
                    update();
                } catch (DataNotInitializedException e) {
                    JOptionPane.showOptionDialog(null, e.getMessage(),
                            "Warning", JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE, null, null, null);
                }
            }

        };

        /**
         * Action for reseting the underlying network.
         */
        private Action resetAction = new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Reset.png"));
                putValue(NAME, "Reset");
                putValue(SHORT_DESCRIPTION, "Reset network");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                network.getSom().reset();
                trainer.setIteration(0);
                panel.getNetwork().fireNetworkChanged();
            }
        };
        /**
         * Action for randomizing the underlying network.
         */
        private Action randomizeAction = new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Rand.png"));
                putValue(NAME, "Randomize");
                putValue(SHORT_DESCRIPTION, "Randomize network");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                network.getSom().randomizeIncomingWeights();
                update();
                panel.getNetwork().fireNetworkChanged();
            }
        };
    }

}
