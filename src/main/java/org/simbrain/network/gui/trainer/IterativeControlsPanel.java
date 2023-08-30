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

import org.simbrain.network.trainers.ErrorListener;
import org.simbrain.network.trainers.IterableTrainer;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.SwingKt;
import org.simbrain.util.Utils;

import javax.swing.*;
import java.util.concurrent.Executors;

/**
 * The main controller panel for iterative learning, with buttons etc. to run
 * the trainer. Wraps a trainer object. Used in conjunction with a training set
 * panel in the iterative training panel.
 * <p>
 * Iterative abstracts over different iterative algorithms, via IterableTrainer.
 *
 * @author Jeff Yoshimi
 */
public class IterativeControlsPanel extends JPanel {

    /**
     * Reference to trainer object.
     */
    private IterableTrainer trainer;

    /**
     * Current number of iterations.
     */
    private JLabel iterationsLabel = new JLabel("--- ");

    /** Flag for showing updates in GUI. */
    //private final JCheckBox showUpdates = new JCheckBox("Show updates");

    /**
     * Error progress bar.
     */
    private JProgressBar errorBar;

    /** Validation progress bar. */
    // private JProgressBar validationBar;

    /**
     * Number of "ticks" in progress bars.
     */
    private int numTicks = 1000;

    /**
     * The error listener.
     */
    private ErrorListener errorListener;

    /**
     * A play action that repeatedly iterates training algorithms.
     */
    private Action runAction = Utils.createAction("Run", "Iterate training until stop button is pressed.", "menu_icons/Play.png", this::run);

    /**
     * A step action that iterates learning algorithms one time.
     */
    private Action stepAction = Utils.createAction("Iterate", "Iterate training once.", "menu_icons/Step.png", this::iterate);

    /**
     * Action for randomizing the underlying network.
     */
    private Action randomizeAction = Utils.createAction("Randomize", "Randomize network.", "menu_icons/Rand.png", this::randomizeNetwork);

    /**
     * Action for setting properties of the trainer.
     */
    private Action setPropertiesAction = Utils.createAction("Properties", "Edit trainer properties.", "menu_icons/Prefs.png", this::editTrainerProperties);


    /**
     * Construct the panel.
     *
     * @param trainer      the trainer this panel represents
     */
    public IterativeControlsPanel(IterableTrainer trainer) {
        this.trainer = trainer;

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
        //showUpdates.setToolTipText(
        //    "Update display of network while iterating trainer (slows performance but didactically useful)");
        //runTools.add(showUpdates);
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
        trainer.getEvents().getErrorUpdated().on((error) -> {
            iterationsLabel.setText("" + trainer.getIteration());
            updateError();
        });
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

    ///**
    // * @return the trainer
    // */
    //public IterableTrainer getTrainer() {
    //    return trainer;
    //}
    //
    ///**
    // * @param trainer the trainer to set
    // */
    //public void setTrainer(IterableTrainer trainer) {
    //    this.trainer = trainer;
    //}

    private void run() {
        if (trainer.isUpdateCompleted()) {
            //TODO: Relation to stop trainer
            runAction.putValue(Action.SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Stop.png"));
            startRunning();
        } else {
            runAction.putValue(Action.SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Play.png"));
            stopRunning();
        }
    }

    private void startRunning() {
        initTrainer(false);
        trainer.setUpdateCompleted(false);
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                while (!trainer.isUpdateCompleted()) {
                    trainer.apply();
                }
            } catch (IterableTrainer.DataNotInitializedException e) {
                JOptionPane.showOptionDialog(null, e.getMessage(), "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            }
        });
    }

    private void stopRunning() {
        trainer.setUpdateCompleted(true);
        trainer.commitChanges();
    }

    private void iterate() {
        initTrainer(false);
        try {
            trainer.apply();
            // trainer.revalidateSynapseGroups();
        } catch (IterableTrainer.DataNotInitializedException e) {
            JOptionPane.showOptionDialog(null, e.getMessage(), "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
        }
    }

    private void randomizeNetwork() {
        initTrainer(true);
        trainer.randomize();
    }

    private void editTrainerProperties() {
        var dialog = SwingKt.createDialog(trainer);
        SwingKt.display(dialog);
    }


}