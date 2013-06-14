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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
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
import org.simbrain.network.trainers.TrainerListener;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;

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
    private JProgressBar validationBar;

    /** Number of "ticks" in progress bars. */
    private int numTicks = 1000;

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
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Controls"));
        GridBagConstraints controlPanelConstraints = new GridBagConstraints();

        // Run Tools
        JPanel runTools = new JPanel();
        runTools.add(new JButton(runAction));
        runTools.add(new JButton(stepAction));
        runTools.add(showUpdates);
        controlPanelConstraints.weightx = 0.5;
        controlPanelConstraints.gridx = 0;
        controlPanelConstraints.gridy = 0;
        add(runTools, controlPanelConstraints);

        // Separator
        controlPanelConstraints.weightx = 1;
        controlPanelConstraints.weighty = 1;
        controlPanelConstraints.gridx = 0;
        controlPanelConstraints.gridy = 1;
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(200, 15));
        // separator.setBackground(Color.red);
        add(separator, controlPanelConstraints);

        // Labels
        LabelledItemPanel labelPanel = new LabelledItemPanel();
        labelPanel.addItem("Iterations:", iterationsLabel);
        numTicks = 10;
        errorBar = new JProgressBar(0, numTicks);
        errorBar.setStringPainted(true);
        labelPanel.addItem("Error:", errorBar);
        validationBar = new JProgressBar(0, numTicks);
        validationBar.setStringPainted(true);
        labelPanel.addItem("Validation Error:", validationBar);
        controlPanelConstraints.weightx = 0.5;
        controlPanelConstraints.gridx = 0;
        controlPanelConstraints.gridy = 2;
        add(labelPanel, controlPanelConstraints);

        // Separator
        controlPanelConstraints.weightx = 0.5;
        controlPanelConstraints.weighty = 0.5;
        controlPanelConstraints.gridx = 0;
        controlPanelConstraints.gridy = 3;
        JSeparator separator2 = new JSeparator(SwingConstants.HORIZONTAL);
        separator2.setPreferredSize(new Dimension(200, 15));
        // separator.setBackground(Color.red);
        add(separator2, controlPanelConstraints);

        // Button panel at bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton propertiesButton = new JButton(setPropertiesAction);
        propertiesButton.setHideActionText(true);
        buttonPanel.add(propertiesButton);
        JButton randomizeButton = new JButton(randomizeAction);
        randomizeButton.setHideActionText(true);
        buttonPanel.add(randomizeButton);
        JButton plotButton = new JButton(TrainerGuiActions.getShowPlotAction(
                panel, trainer));
        plotButton.setHideActionText(true);
        buttonPanel.add(plotButton);
        controlPanelConstraints.weightx = 0.5;
        controlPanelConstraints.gridx = 0;
        controlPanelConstraints.gridy = 4;
        add(buttonPanel, controlPanelConstraints);

        // Set up control panel
        int width = 290;
        int height = 260;
        setMaximumSize(new Dimension(width, height));
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));

        addErrorListener();
    }

    ErrorListener errorListener;
    //todo
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
        validationBar.setValue((int) (numTicks * trainer.getError()));
        errorBar.setString("" + Utils.round(trainer.getError(), 4));
        validationBar.setString("" + Utils.round(trainer.getError(), 4));
    }

    /**
     * Called whenever the trainer should be reinitialized. Actual trainer
     * initialization happens in subclasses that override this method. If
     * forcereinit is true the training set is recreated. If not some useful
     * data integrity checks still happen. See subclasses in for example
     * SRNTrainingPanel.
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
            // putValue(SHORT_DESCRIPTION, "Import table from .csv");
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
                Executors.newSingleThreadExecutor().submit(new Runnable() {
                    public void run() {
                        try {
                            while (!trainer.isUpdateCompleted()) {
                                trainer.iterate();
                                if (showUpdates.isSelected()) {
                                    panel.getNetwork()
                                            .setUpdateCompleted(false);
                                    panel.getNetwork().fireNetworkChanged();
                                    while (!panel.getNetwork()
                                            .isUpdateCompleted()) {
                                        try {
                                            Thread.sleep(1);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        putValue(SMALL_ICON, ResourceManager
                                                .getImageIcon("Stop.png"));
                                    }
                                }
                                putValue(SMALL_ICON, ResourceManager
                                        .getImageIcon("Play.png"));
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
            // putValue(SHORT_DESCRIPTION, "Import table from .csv");
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
                    panel.getNetwork().fireNetworkChanged();
                }
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
                panel.getNetwork().fireNetworkChanged();
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
            putValue(SHORT_DESCRIPTION, "Edit Properties");
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
            editor.setExcludeList(new String[] { "iteration",
                    "updateCompleted" });
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