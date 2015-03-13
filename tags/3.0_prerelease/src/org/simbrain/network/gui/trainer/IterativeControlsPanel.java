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

import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.trainers.ErrorListener;
import org.simbrain.network.trainers.IterableTrainer;
import org.simbrain.network.trainers.Trainer.DataNotInitializedException;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.gui.ReflectivePropertyEditor;

/**
 * The main controller panel for iterative learning, with buttons etc. to run
 * the trainer. Wraps a trainer object. Used in conjunction with a training set
 * panel in the iterative training panel.
 *
 * Iterative abstracts over different iterative algorithms, via IterableTrainer.
 *
 * @author Jeff Yoshimi
 */
public class IterativeControlsPanel extends JPanel {

    /** Reference to trainer object. */
    private IterableTrainer trainer;

    /** Current number of iterations. */
    private JLabel iterationsLabel = new JLabel("--- ");

    /** Reference to network panel. */
    private final NetworkPanel panel;

    /** Flag for showing updates in GUI. */
    private final JCheckBox showUpdates = new JCheckBox("Show updates");

    /** Error progress bar. */
    private JProgressBar errorBar;

    /** Validation progress bar. */
    // private JProgressBar validationBar;

    /** Number of "ticks" in progress bars. */
    private int numTicks = 1000;

    /** The error listener. */
    private ErrorListener errorListener;

    /**
     * Construct the panel with no trainer. It will be supplied later once it
     * has been created. This is used by SRN and ESN for example where the
     * "logical input data" must first be constructed and only then is the
     * trainer created.
     *
     * @param networkPanel the parent network panel
     */
    public IterativeControlsPanel(final NetworkPanel networkPanel) {
        this.panel = networkPanel;
        init();
    }

    /**
     * Construct the panel with a trainer specified.
     *
     * @param networkPanel the parent network panel
     * @param trainer the trainer this panel represents
     */
    public IterativeControlsPanel(final NetworkPanel networkPanel,
            final IterableTrainer trainer) {

        this.trainer = trainer;
        this.panel = networkPanel;
        init();
    }

    /**
     * Initialize the panel.
     */
    public void init() {

        //setBorder(BorderFactory.createTitledBorder("Controls"));

        // Set up properties tab
        Box propsBox = Box.createVerticalBox();
        propsBox.setOpaque(true);
        propsBox.add(Box.createVerticalGlue());

        // Run Tools
        JPanel runTools = new JPanel();
        runTools.add(new JLabel("Iterate: "));
        runTools.add(new JButton(runAction));
        runTools.add(new JButton(stepAction));
        showUpdates
                .setToolTipText("Update display of network while iterating trainer (slows performance but didactically useful)");
        runTools.add(showUpdates);
        JButton propertiesButton = new JButton(setPropertiesAction);
        propertiesButton.setHideActionText(true);
        runTools.add(propertiesButton);
        JButton randomizeButton = new JButton(randomizeAction);
        randomizeButton.setHideActionText(true);
        runTools.add(randomizeButton);
        propsBox.add(runTools);

        // Separator
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        propsBox.add(separator);

        // Labels
        LabelledItemPanel labelPanel = new LabelledItemPanel();
        labelPanel.addItem("Iterations:", iterationsLabel);
        numTicks = 10;
        errorBar = new JProgressBar(0, numTicks);
        errorBar.setStringPainted(true);
        // errorBar.setMinimumSize(new Dimension(200,100));
        labelPanel.addItem("Error:", errorBar);
        // validationBar = new JProgressBar(0, numTicks);
        // validationBar.setStringPainted(true);
        // labelPanel.addItem("Validation Error:", validationBar);
        propsBox.add(labelPanel);

        // Separator
        JSeparator separator2 = new JSeparator(SwingConstants.HORIZONTAL);
        propsBox.add(separator2);
        propsBox.add(Box.createVerticalStrut(20));

        // Time series for error
        ErrorPlotPanel graphPanel = new ErrorPlotPanel(trainer);
        propsBox.add(graphPanel);

        add(propsBox);
        addErrorListener();
    }

    /**
     * Add an error listener.
     */
    public void addErrorListener() {
        // Add listener
        if (trainer != null) {
            if (errorListener != null) {
                trainer.removeErrorListener(errorListener);
            }
            errorListener = new ErrorListener() {

                public void errorUpdated() {
                    iterationsLabel.setText("" + trainer.getIteration());
                    updateError();
                }

            };
            trainer.addErrorListener(errorListener);
        }
    }

    /**
     * Update the error field.
     */
    private void updateError() {
        errorBar.setValue((int) (numTicks * trainer.getError()));
        // validationBar.setValue((int) (numTicks * trainer.getError()));
        errorBar.setString("" + Utils.round(trainer.getError(), 4));
        // validationBar.setString("" + Utils.round(trainer.getError(), 4));
    }

    /**
     * Called whenever the trainer should be reinitialized. Actual trainer
     * initialization happens in subclasses that override this method. If
     * forcereinit is true the training set is recreated. If not some useful
     * data integrity checks still happen. (Not currently used).
     *
     * @param forceReinit whether to require that data be reinitialized
     */
    protected void initTrainer(boolean forceReinit) {
    }

    /**
     * A "play" action, that can be used to repeatedly iterate iterable training
     * algorithms.
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
            initTrainer(false);
            if (trainer == null) {
                return;
            }
            if (trainer.isUpdateCompleted()) {
                // Start running
                trainer.setUpdateCompleted(false);
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Stop.png"));
                Executors.newSingleThreadExecutor().submit(new Runnable() {
                    public void run() {
                        try {
                            while (!trainer.isUpdateCompleted()) {
                                trainer.iterate();
                                if (showUpdates.isSelected()) {
                                    panel.getNetwork()
                                            .setUpdateCompleted(false);
                                    panel.getNetwork().fireGroupUpdated(
                                            trainer.getTrainableNetwork()
                                                    .getNetwork());
                                    while (!panel.getNetwork()
                                            .isUpdateCompleted()) {
                                        try {
                                            Thread.sleep(1);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } catch (DataNotInitializedException e) {
                            JOptionPane.showOptionDialog(null, e.getMessage(),
                                    "Warning", JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.WARNING_MESSAGE, null, null,
                                    null);
                        }
                    }
                });
            } else {
                // Stop running
                trainer.setUpdateCompleted(true);
                trainer.revalidateSynapseGroups();
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Play.png"));
            }

        }

    };

    /**
     * A step action, for iterating iteratable learning algorithms one time.
     */
    private Action stepAction = new AbstractAction() {

        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Step.png"));
            // putValue(NAME, "Open (.csv)");
            putValue(SHORT_DESCRIPTION, "Iterate training once");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            initTrainer(false);
            if (trainer == null) {
                return;
            }
            try {
                trainer.iterate();
                if (showUpdates.isSelected()) {
                    panel.getNetwork().fireGroupUpdated(
                            trainer.getTrainableNetwork().getNetwork());
                }
                trainer.revalidateSynapseGroups();
            } catch (DataNotInitializedException e) {
                JOptionPane.showOptionDialog(null, e.getMessage(), "Warning",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE, null, null, null);
            }
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
            initTrainer(true);
            if (trainer != null) {
                trainer.randomize();
                panel.getNetwork().fireGroupUpdated(
                        trainer.getTrainableNetwork().getNetwork());
            }
        }
    };

    /**
     * Action for setting properties.
     */
    private Action setPropertiesAction = new AbstractAction() {

        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
            putValue(NAME, "Properties");
            putValue(SHORT_DESCRIPTION, "Edit Trainer Settings...");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            initTrainer(false);
            if (trainer == null) {
                return;
            }
            ReflectivePropertyEditor editor = new ReflectivePropertyEditor();
            editor.setExcludeList(new String[] { "iteration", "updateCompleted" });
            editor.setObject(trainer);
            JDialog dialog = editor.getDialog();
            dialog.setModal(true);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        }
    };

    /**
     * @return the trainer
     */
    public IterableTrainer getTrainer() {
        return trainer;
    }

    /**
     * @param trainer the trainer to set
     */
    public void setTrainer(IterableTrainer trainer) {
        this.trainer = trainer;
    }

}