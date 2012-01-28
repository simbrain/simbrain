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

import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.trainers.IterableAlgorithm;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.network.trainers.TrainerListener;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.Utils;

/**
 * Component for choosing what kind of supervised learning to use. Can be
 * initialized with a set or trainer types.
 * 
 * @author Jeff Yoshimi
 */
public class IterativeControlsPanel extends JPanel {

    /** Reference to trainer object. */
    private final Trainer trainer;

    /** Current number of iterations. */
    private JLabel iterationsLabel = new JLabel("  ");
    
    /** Error label. */
    private JLabel rmsError = new JLabel("Error: ----- ");

    /** Iterations thus far. */ 
    private int iterations = 0; //TODO: A way to set to 0

    /**
     * Construct a rule chooser panel.
     *
     * @param networkPanel the parent network panel
     * @param trainer the trainer this panel represents
     */
    public IterativeControlsPanel(final NetworkPanel networkPanel, final Trainer trainer) {

        this.trainer = trainer;
        JPanel mainPanel = new JPanel();
        
        // Run
        mainPanel.add(new JButton(runAction));
        mainPanel.add(new JButton(stepAction));
        //runningLabel.setIcon(ResourceManager.getImageIcon("Throbber.gif"));
        //mainPanel.add(runningLabel);

        // Batch
        // iterationPanel.add(new
        // JButton(TrainerGuiActions2.getBatchTrainAction(this)));

        // Iterations
         mainPanel.add(new JLabel("Iterations:"));
         mainPanel.add(iterationsLabel);

        // Error
        mainPanel.add(rmsError);

        // Randomize (de-activate depending...)
        mainPanel.add(new JButton(TrainerGuiActions
                .getRandomizeNetworkAction(trainer)));
        add(mainPanel);
        
        // Add listener
        trainer.addListener(new TrainerListener() {

            public void errorUpdated() {
                iterations++;
                iterationsLabel.setText("" + iterations + " ");
                updateError();
            }

            public void inputDataChanged(double[][] inputData) {
            }

            public void trainingDataChanged(double[][] trainingData) {
            }
            
        });


    }
    
    /**
     * Update the error field.
     */
    private void updateError() {
        rmsError.setText("Error:"
                + Utils.round(((IterableAlgorithm) trainer
                        .getTrainingMethod()).getError(), 4));
    }

    /**
     * A "play" action, that can be used to repeatedly iterate iterable
     * training algorithms.
     * 
     */
    Action runAction = new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Play.png"));
                // putValue(NAME, "Open (.csv)");
                // putValue(SHORT_DESCRIPTION, "Import table from .csv");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                if (trainer.isUpdateCompleted()) {
                    // Start running
                    trainer.setUpdateCompleted(false);
                    Executors.newSingleThreadExecutor().submit(new Runnable() {
                        public void run() {
                            while (!trainer.isUpdateCompleted()) {
                                trainer.update();
                                // TODO: Make below an option?
                                // trainerGui.getTrainer().getNetwork().getRootNetwork().fireNetworkChanged();
                            }
                            {
                                putValue(SMALL_ICON, ResourceManager
                                        .getImageIcon("Play.png"));
                            }
                        }
                    });
                    putValue(SMALL_ICON,
                            ResourceManager.getImageIcon("Stop.png"));
                } else {
                    // Stop running
                    trainer.setUpdateCompleted(true);
                    putValue(SMALL_ICON,
                            ResourceManager.getImageIcon("Play.png"));
                }

            }

        };

    /**
     * A step action, for iterating iteratable learning algorithms one
     * time.
     */
    Action stepAction = new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Step.png"));
                // putValue(NAME, "Open (.csv)");
                // putValue(SHORT_DESCRIPTION, "Import table from .csv");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                trainer.update();
            }

        };

    /**
     * A batch training action.
     */
    Action batchTrain = new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON,
                        ResourceManager.getImageIcon("BatchPlay.png"));
                // putValue(NAME, "Batch");
                putValue(SHORT_DESCRIPTION, "Batch train network");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                // TODO
            }

        };

}
