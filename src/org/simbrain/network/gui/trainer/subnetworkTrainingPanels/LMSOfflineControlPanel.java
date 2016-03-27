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
package org.simbrain.network.gui.trainer.subnetworkTrainingPanels;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.LMSOffline.SolutionType;
import org.simbrain.network.trainers.Trainer.DataNotInitializedException;
import org.simbrain.network.trainers.TrainerListener;
import org.simbrain.util.randomizer.gui.RandomizerPanel;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;

/**
 * Control panel (buttons etc.) for least mean squares offline trainers. Used in
 * conjunction with a trainingset panel in the LMSOfflineTrainingPanel.
 *
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class LMSOfflineControlPanel extends JPanel {

    /** Reference to trainer object. */
    private LMSOffline trainer;

    /** The button which starts training. */
    private JButton applyButton = new JButton("Start");

    /** The progress bar, tracking the progress of training. */
    private final JProgressBar progressBar = new JProgressBar();

    /** A combo box containing the supported solution types. */
    private final JComboBox<SolutionType> solutionTypes =
        new JComboBox<SolutionType>(SolutionType.values());

    // This is flashy a checkbox could do the work... but this is more pretty...
    /** A button regulating the use of ridge regression. */
    private JButton regSwitch = new JButton("Off");

    private JCheckBox ridgeRegChkBx = new JCheckBox();

    /** A flag for whether or not regression is being used. */
    private boolean regressionActive;

    // Initialize pretty button...
    {
        regSwitch.setForeground(Color.red);
    }

    /** A text field containing the alpha value. */
    private JTextField alpha = new JTextField(" 0.5 ", 10);

    {
        alpha.setMaximumSize(alpha.getPreferredSize());
    }

    private RandomizerPanel noisePanel;

    private DropDownTriangle noiseTri;

    private JCheckBox noiseChkBx = new JCheckBox();

    /** Parent frame. */
    private final Window frame;

    /**
     * Build the panel. No trainer is supplied. It will be supplied later once
     * it has been created. This is used by ESN for example where states must
     * first be harvested and only then is the trainer built.
     *
     * @param frame the parent network panel
     */
    public LMSOfflineControlPanel(Window frame) {
        this.frame = frame;
        noiseTri = new DropDownTriangle(UpDirection.LEFT, false, "", "", frame);
        noisePanel = new RandomizerPanel(frame);
        noisePanel.fillDefaultValues();
        init();
    }

    /**
     * Build the panel.
     *
     * @param frame the parent network panel
     * @param trainer the LMSOffline trainer to represent
     */
    public LMSOfflineControlPanel(final LMSOffline trainer, Window frame) {
        this.frame = frame;
        this.trainer = trainer;
        noisePanel = new RandomizerPanel(frame);
        noisePanel.fillFieldValues(trainer.getNoiseGen());
        noiseTri = new DropDownTriangle(UpDirection.LEFT, false, "", "", frame);
        String text = noisePanel.getSummary();
        noiseTri.setBothTexts(text, text);
        init();
    }

    /**
     * Initialized the panel.
     */
    private void init() {

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(Box.createVerticalStrut(10));

        // Solution type
        Box solType = Box.createHorizontalBox();
        solType.add(new JLabel("Solution Type: "));
        solType.add(Box.createHorizontalGlue());
        solType.add(Box.createHorizontalStrut(100));
        solType.add(solutionTypes);
        this.add(solType);
        this.add(Box.createVerticalStrut(10));

        // Ridge Regression:
        Box rReg = Box.createHorizontalBox();
        rReg.add(new JLabel("Ridge Regression: "));
        rReg.add(ridgeRegChkBx);
        rReg.add(Box.createHorizontalGlue());
        rReg.add(alpha);
        alpha.setEnabled(ridgeRegChkBx.isSelected());
        this.add(rReg);
        this.add(Box.createVerticalStrut(10));

        // Noise
        Box noiseBx = Box.createHorizontalBox();
        noiseBx.add(new JLabel("Noise: "));
        noiseBx.add(noiseChkBx);
        noiseBx.add(Box.createHorizontalGlue());
        noiseBx.add(noiseTri);
        String text = noisePanel.getSummary();
        noiseTri.setBothTexts(text, text);
        this.add(noiseBx);
        this.add(Box.createVerticalStrut(5));

        // Noise panel
        noisePanel.setVisible(noiseTri.isDown());
        noisePanel.setEnabled(noiseChkBx.isSelected());
        this.add(noisePanel);
        this.add(Box.createVerticalStrut(5));

        Box applyPanel = Box.createHorizontalBox();
        applyPanel.add(Box.createHorizontalStrut(5));
        applyPanel.add(progressBar);
        applyPanel.add(Box.createHorizontalGlue());
        applyPanel.add(Box.createHorizontalStrut(15));
        applyPanel.add(applyButton);
        applyPanel.add(Box.createHorizontalStrut(5));
        this.add(applyPanel);

        addActionListeners();

        if (trainer != null) {
            addTrainerListeners();
        }

    }

    public void addMouseListenerToTriangle(MouseAdapter ma) {
        noiseTri.addMouseListener(ma);
    }

    /**
     * Adds all the necessary action listeners, including listeners for:
     * trainer, applyButton, solutionTypes, and regSwitch.
     */
    private void addActionListeners() {
        // If focus is lost from one of the fields in the noise panel
        // change the summary description.
        noisePanel.addFocusListenerToFields(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
            }

            @Override
            public void focusLost(FocusEvent arg0) {
                String text = noisePanel.getSummary();
                noiseTri.setBothTexts(text, text);
            }
        });

        // If the noise distribution has been changed, change the summary
        // description.
        noisePanel.getCbDistribution().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String text = noisePanel.getSummary();
                        noiseTri.setBothTexts(text, text);
                    }
                });
            }
        });

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
                trainer.setSolutionType((SolutionType) solutionTypes
                    .getSelectedItem());

                // Disables/enabled ridge regression based on selected solution
                if (solutionTypes.getSelectedItem()
                == LMSOffline.SolutionType.MOORE_PENROSE) {
                    ridgeRegChkBx.setSelected(false);
                    ridgeRegChkBx.setEnabled(false);
                    alpha.setEnabled(false);
                } else {
                    ridgeRegChkBx.setEnabled(true);
                    alpha.setEnabled(true);
                }
            }

        });

        ridgeRegChkBx.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                alpha.setEnabled(ridgeRegChkBx.isSelected());
            }
        });

        noiseChkBx.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                noisePanel.setEnabled(noiseChkBx.isSelected());
                noiseTri.setEnabled(noiseChkBx.isSelected());
            }
        });

        noiseTri.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                noisePanel.setVisible(noiseTri.isDown());
                noisePanel.repaint();
                noisePanel.revalidate();
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
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                setCursor(null); // Turn off wait cursor

                // BELOW causes hard crash
                //                networkPanel.getNetwork().fireGroupUpdated(
                //                        ((LMSNetwork) trainer.getTrainableNetwork()
                //                                .getNetwork()).getSynapseGroup());
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
        trainer.setNoiseAdded(noiseChkBx.isSelected());
        if (noiseChkBx.isSelected()) {
            noisePanel.commitRandom(trainer.getNoiseGen());
        }
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            public Void doInBackground() {
                try {
                    trainer.apply();
                } catch (DataNotInitializedException e) {
                    JOptionPane.showOptionDialog(null, e.getMessage(),
                        "Warning", JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE, null, null, null);
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

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        LMSOfflineControlPanel locp = new LMSOfflineControlPanel(frame);
        frame.setContentPane(locp);
        frame.setVisible(true);
        frame.pack();
    }

}
