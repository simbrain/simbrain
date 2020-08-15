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
import org.simbrain.network.trainers.LMSIterative;
import org.simbrain.network.trainers.Trainer.DataNotInitializedException;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;

import javax.swing.*;
import java.util.concurrent.Executors;

/**
 * Experimental.  For now just edit an LMSIterativeObject on its own.
 */
public class IterativeControlsPanel2 extends JPanel {

    /**
     * Reference to the LMS object.
     */
    private LMSIterative lms;

    /**
     * Current number of iterations.
     */
    private JLabel iterationsLabel = new JLabel("--- ");

    /**
     * Error progress bar.
     */
    private JProgressBar errorBar;

    /**
     * Number of "ticks" in progress bars.
     */
    private int numTicks = 1000;

    // TODO.
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
     */
    public IterativeControlsPanel2(LMSIterative lms) {
        this.lms = lms;

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
        propsBox.add(labelPanel);

        // Separator
        JSeparator separator2 = new JSeparator(SwingConstants.HORIZONTAL);
        propsBox.add(separator2);
        propsBox.add(Box.createVerticalStrut(20));

        // Time series for error
        ErrorPlotPanel graphPanel = new ErrorPlotPanel(lms);
        propsBox.add(graphPanel);

        add(propsBox);
        lms.getEvents().onErrorUpdated(() -> {
            iterationsLabel.setText("" + lms.getIteration());
            updateError();
        });
    }

    /**
     * Update the error field.
     */
    private void updateError() {
        errorBar.setValue((int) (numTicks * lms.getError()));
        // validationBar.setValue((int) (numTicks * trainer.getError()));
        errorBar.setString("" + Utils.round(lms.getError(), 4));
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

    private void run() {
        if (lms.isUpdateCompleted()) {
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
        lms.setUpdateCompleted(false);
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                while (!lms.isUpdateCompleted()) {
                    lms.apply();
                }
            } catch (IterableTrainer.DataNotInitializedException e) {
                JOptionPane.showOptionDialog(null, e.getMessage(), "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            }
        });
    }

    private void stopRunning() {
        lms.setUpdateCompleted(true);
        // lms.revalidateSynapseGroups();
        lms.commitChanges();
    }

    private void iterate() {
        initTrainer(false);
        try {
            lms.apply();
            // lms.revalidateSynapseGroups();
        } catch (IterableTrainer.DataNotInitializedException e) {
            JOptionPane.showOptionDialog(null, e.getMessage(), "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
        }
    }

    private void randomizeNetwork() {
        initTrainer(true);
        lms.randomize();
    }

    private void editTrainerProperties() {
        AnnotatedPropertyEditor trainerProps = new AnnotatedPropertyEditor(lms);
        StandardDialog dialog = trainerProps.getDialog();
        dialog.pack();
        dialog.setVisible(true);
    }


}