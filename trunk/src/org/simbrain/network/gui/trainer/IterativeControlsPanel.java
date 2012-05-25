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
package org.simbrain.network.gui.trainer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.trainers.ErrorListener;
import org.simbrain.network.trainers.IterableTrainer;
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
    private final IterableTrainer trainer;

    /** Current number of iterations. */
    private JLabel iterationsLabel = new JLabel("--- ");
    
    /** Error label. */
    private JLabel rmsError = new JLabel("Error: --- ");

    /** Batch iterations field. */
    private JTextField tfIterations = new JTextField("1000");
    
    /** Iterations thus far. */ 
    private int iterations = 0; //TODO: A way to set to 0

    /** Reference to network panel. */
    private final NetworkPanel panel;
    
    /** Flag for showing updates in GUI. */
    private final JCheckBox showUpdates = new JCheckBox("Show updates");
    
    /**
     * Construct a rule chooser panel.
     *
     * @param networkPanel the parent network panel
     * @param trainer the trainer this panel represents
     */
    public IterativeControlsPanel(final NetworkPanel networkPanel, final IterableTrainer trainer) {

        this.trainer = trainer;
        this.panel = networkPanel;
        setLayout(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Run
        JPanel runPanel = new JPanel();
        runPanel.add(new JButton(runAction));
        runPanel.add(new JButton(stepAction));
        runPanel.add(showUpdates);
        tabbedPane.addTab("Run", runPanel);

		// Batch train
		JPanel batchPanel = new JPanel();
		batchPanel.add(tfIterations);
		JButton batchButton = new JButton(batchTrain);
		batchButton.setHideActionText(true);
		batchPanel.add(batchButton);
		tabbedPane.addTab("Batch", batchPanel);

        // South Panel
		JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		southPanel.add(new JLabel("Iterations:"));
		southPanel.add(iterationsLabel);
		southPanel.add(rmsError);
    	JButton plotButton = new JButton(TrainerGuiActions.getShowPlotAction(networkPanel, trainer));
    	plotButton.setHideActionText(true);
    	southPanel.add(plotButton);

        add("Center", tabbedPane);
        add("South", southPanel);
        
        // Add listener
        trainer.addErrorListener(new ErrorListener() {

            public void errorUpdated() {
                iterations++;
                iterationsLabel.setText("" + iterations + " ");
                updateError();
            }
            
        });

    }
    
    /**
     * Update the error field.
     */
    private void updateError() {
        rmsError.setText("Error:"
                + Utils.round(trainer.getError(), 4));
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
							trainer.apply();
							if (showUpdates.isSelected()) {
								panel.getNetwork()
										.setUpdateCompleted(false);
								panel.getNetwork().fireNetworkChanged();
								while (panel.getNetwork()
										.isUpdateCompleted() == false) {
									try {
										Thread.sleep(1);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
							}
						}
						{
							putValue(SMALL_ICON,
									ResourceManager.getImageIcon("Play.png"));
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
                trainer.apply();
                if (showUpdates.isSelected()) {
                	panel.getNetwork().fireNetworkChanged();
                }
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
                putValue(NAME, "Batch");
                putValue(SHORT_DESCRIPTION, "Batch train network");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                trainer.iterate(Integer.parseInt(tfIterations.getText()));
            }

        };
    

}