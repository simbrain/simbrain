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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.Trainer.DataNotInitializedException;
import org.simbrain.network.trainers.TrainerListener;
import org.simbrain.util.genericframe.GenericFrame;

/**
 * Control panel (buttons etc.) for least mean squares offline trainers. Used in
 * conjunction with a trainingset panel in the LMSOfflineTrainingPanel.
 *
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class LMSOfflineControlPanel extends JPanel {

    /** Reference to trainer object. */
    private LMSOffline trainer;

    /** Reference to network panel. */
    private final NetworkPanel networkPanel;

    /**
     * The main LMS panel, where the start button, progress bar, and solution
     * box reside.
     */
    private JPanel mainPanel = new JPanel();

    /** The panel devoted to ridge regression. */
    private JPanel regressionPanel = new JPanel();

    /** The button which starts training. */
    private JButton applyButton = new JButton("Start");

    /** The progress bar, tracking the progress of training. */
    private final JProgressBar progressBar = new JProgressBar();

    /** The solution types as strings within the combo box. */
    private String[] solutions = { "Wiener-Hopf", "Psuedoinverse" };

    /** A combo box containing the supported solution types. */
    private final JComboBox solutionTypes = new JComboBox(solutions);

    /** A hashmap backing the solution type combo box. */
    private final HashMap<String, LMSOffline.SolutionType> pairing = new HashMap<String, LMSOffline.SolutionType>();

    // Initializing the solution type hash map...
    {
        pairing.put("Wiener-Hopf", LMSOffline.SolutionType.WIENER_HOPF);
        pairing.put("Psuedoinverse", LMSOffline.SolutionType.MOORE_PENROSE);
    }

    // This is flashy a checkbox could do the work... but this is more pretty...
    /** A button regulating the use of ridge regression. */
    private JButton regSwitch = new JButton("Off");

    /** A flag for whether or not regression is being used. */
    private boolean regressionActive;

    // Initialize pretty button...
    {
        regSwitch.setForeground(Color.red);
    }

    /** A text field containing the alpha value. */
    private JTextField alpha = new JTextField(" 0.5 ");

    /** Parent frame. */
    private GenericFrame frame;

    /**
     * Build the panel. No trainer is supplied.  It will be supplied
     * later once it has been created.  This is used by ESN for example
     * where states must first be harvested and only then is the trainer
     * built.
     *
     * @param networkPanel the parent network panel
     */
    public LMSOfflineControlPanel(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        init();
    }

    /**
     * Build the panel.
     *
     * @param networkPanel the parent network panel
     * @param trainer the LMSOffline trainer to represent
     */
    public LMSOfflineControlPanel(final NetworkPanel networkPanel,
            final LMSOffline trainer) {
        this.networkPanel = networkPanel;
        this.trainer = trainer;
        init();
    }

    /**
     * Initialized the panel.
     */
    private void init() {

        // Set up main controls
        setLayout(new GridBagLayout());
        GridBagConstraints controlPanelConstraints = new GridBagConstraints();
        setBorder(BorderFactory.createTitledBorder("Controls"));

        // Main panel
        fillMainPanel();
        controlPanelConstraints.weightx = 0.5;
        controlPanelConstraints.gridx = 0;
        controlPanelConstraints.gridy = 0;
        add(mainPanel, controlPanelConstraints);

        // Separator
        controlPanelConstraints.weightx = 1;
        controlPanelConstraints.weighty = 1;
        controlPanelConstraints.gridx = 0;
        controlPanelConstraints.gridy = 1;
        JSeparator separator2 = new JSeparator(SwingConstants.HORIZONTAL);
        separator2.setPreferredSize(new Dimension(250, 15));
        add(separator2, controlPanelConstraints);

        // Regression
        fillRegressionPanel();
        controlPanelConstraints.weightx = 1;
        controlPanelConstraints.weighty = 1;
        controlPanelConstraints.gridx = 0;
        controlPanelConstraints.gridy = 2;
        controlPanelConstraints.insets = new Insets(5, 15, 5, 15);
        add(regressionPanel, controlPanelConstraints);

        addActionListeners();

        if (trainer != null) {
            addTrainerListeners();
        }

    }

    /**
     * Creates the main panel, containing the start button, progress bar, and
     * solution type selector.
     */
    private void fillMainPanel() {
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 5));
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);
        row1.add(applyButton);
        row1.add(progressBar);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 5));
        row2.add(new JLabel("Solution type"));
        row2.add(solutionTypes);
        mainPanel.setLayout(new GridLayout(2, 1));
        mainPanel.add(row1);
        mainPanel.add(row2);
    }

    /**
     * Creates the regression panel, containing the regression switch, and a
     * field to set alpha levels.
     */
    private void fillRegressionPanel() {
        regressionPanel.setLayout(new GridLayout(1,3));
        regressionPanel.add(new JLabel("Ridge regression"));
        regressionPanel.add(regSwitch);
        alpha.setEnabled(false);
        regressionPanel.add(alpha);
    }

    /**
     * Adds all the necessary action listeners, including listeners for:
     * trainer, applyButton, solutionTypes, and regSwitch.
     */
    private void addActionListeners() {
        // Adds a listener for the start button: executes training upon firing.
        // Also activates ridge regression in LMSOffline and sets the alpha.
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (trainer != null) {
                    runTrainer();
                }
            }
        });

        // Combo-box listener...
        solutionTypes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {

                // Reset progress bar
                progressBar.setValue(0);

                if (trainer == null) {
                    return;
                }

                // Sets the solution type in LMSOffline
                trainer.setSolutionType(pairing.get(solutionTypes
                        .getSelectedItem()));

                // Disables/enabled ridge regression based on selected solution
                if (pairing.get(solutionTypes.getSelectedItem()) == LMSOffline.SolutionType.MOORE_PENROSE) {
                    regSwitch.setText("Off");
                    regSwitch.setForeground(Color.red);
                    regSwitch.setEnabled(false);
                    alpha.setEnabled(false);
                    regressionActive = false;
                } else {
                    regSwitch.setEnabled(true);
                }
            }

        });

        // Ridge regression button listener
        regSwitch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // Reset progress bar
                progressBar.setValue(0);

                if (regressionActive) {
                    regressionActive = false;
                    regSwitch.setText("Off");
                    regSwitch.setForeground(Color.red);
                    alpha.setEnabled(false);
                } else {
                    regressionActive = true;
                    regSwitch.setText("On");
                    regSwitch.setForeground(Color.green);
                    alpha.setEnabled(true);
                }
                frame.pack();
            }

        });

    }

    /**
     * Add trainer listeners.
     */
    public void addTrainerListeners() {

        // Adds listeners which update the progress bars values during training
        trainer.addListener(new TrainerListener() {

            @Override
            public void beginTraining() {
                // System.out.println("Training Begin");
                // progressBar.setIndeterminate(true);
                progressBar.setValue(0);
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }

            @Override
            public void endTraining() {
                // System.out.println("Training End");
                // progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                setCursor(null); // Turn off wait cursor
                networkPanel.getNetwork().fireNetworkChanged();
            }

            @Override
            public void progressUpdated(String progressUpdate,
                    int percentComplete) {
                // System.out.println(progressUpdate + " -- " + percentComplete
                // + "%");
                progressBar.setValue(percentComplete);
                // progressBar.setString(progressUpdate);
            }
        });
    }

    // Various things being made visible here so ESN can set the training panel
    // after the first click of the "apply" button

    /**
     * Run the trainer.
     */
    public void runTrainer() {
        trainer.setRidgeRegression(regressionActive);
        if (regressionActive) {
            trainer.setAlpha(Double.parseDouble(alpha.getText()));
        }
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            public Void doInBackground() {
                try {
                    trainer.apply();
                } catch (DataNotInitializedException e) {
                    JOptionPane.showOptionDialog(null, e.getMessage(),
                            "Warning", JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE, null, null,
                            null);
                }
                return null;
            }
        };
        worker.execute();
    }

    /**
     * ESN must set the trainer because when the ESN training panel is opened
     * the trainer has not been created yet.
     *
     * @param trainer the trainer to set
     */
    public void setTrainer(LMSOffline trainer) {
        this.trainer = trainer;
    }

    /**
     * @return the trainer
     */
    public LMSOffline getTrainer() {
        return trainer;
    }

    /**
     * @return the applyButton
     */
    public JButton getApplyButton() {
        return applyButton;
    }

    /**
     * @param frame the parentFrame to set
     */
    public void setFrame(GenericFrame frame) {
        this.frame = frame;
    }

}
