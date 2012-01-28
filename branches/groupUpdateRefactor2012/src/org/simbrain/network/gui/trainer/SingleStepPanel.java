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
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.util.Utils;

/**
 * Component for training a network using the current data only.
 * 
 * @author Jeff Yoshimi
 */
public class SingleStepPanel extends JPanel {

    /**
     * Construct panel.
     */
    public SingleStepPanel(final Trainer trainer) {
        final JLabel currentInput = new JLabel("Input: ");
        final JLabel currentTraining = new JLabel("Training: ");

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JButton setDataButton = new JButton("Set Data");
        setDataButton.setAlignmentX(CENTER_ALIGNMENT);
        currentInput.setAlignmentX(CENTER_ALIGNMENT);
        currentTraining.setAlignmentX(CENTER_ALIGNMENT);
        this.add(setDataButton);
        this.add(currentInput);
        this.add(currentTraining);
        setDataButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                double[] inputVector = Network
                        .getActivationVector(trainer.getInputLayer());
                double[] trainingVector = Network
                        .getActivationVector(trainer.getOutputLayer());
                currentInput.setText("Input: "
                        + Utils.getVectorString(inputVector, ","));
                currentTraining.setText("Target:  "
                        + Utils.getVectorString(trainingVector, ","));
                trainer.setInputData(new double[][] { inputVector});
                trainer.setTrainingData(new double[][] { trainingVector});
                //parentFrame.pack();
            }
        });
    }

}
