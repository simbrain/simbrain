/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.gauge.graphics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.gauge.GaugePreferences;
import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.core.Settings;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;


/**
 * <b>DialogGeneral</b> is a dialog box for setting general Gauge properties.
 */
public class DialogGeneral extends StandardDialog implements ActionListener {
    private GaugePanel theGaugePanel;
    private JTextField perturbationFactor = new JTextField();
    private JTextField tolerance = new JTextField();
    private JComboBox addMethod = new JComboBox(Settings.addMethods);
    private JComboBox defaultProjector = new JComboBox(Gauge.getProjectorList());
    private JButton defaultButton = new JButton("Restore defaults");
    private LabelledItemPanel myContentPane = new LabelledItemPanel();

    /**
     * This method is the default constructor.
     */
    public DialogGeneral(GaugePanel gp) {
        theGaugePanel = gp;
        init();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        setTitle("General Dialog");

        fillFieldValues();
        myContentPane.setBorder(BorderFactory.createEtchedBorder());

        myContentPane.addItem("Only add new point if at least this far from any other point", tolerance);
        myContentPane.addItem("Degree to which to perturb overlapping low-dimensional points", perturbationFactor);
        myContentPane.addItem("Method for adding new datapoints", addMethod);
        myContentPane.addItem("Default Projector", defaultProjector);

        defaultButton.addActionListener(this);
        addButton(defaultButton);
        setContentPane(myContentPane);
    }

    /**
     * Respond to button pressing events
     */
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();

        if (o == defaultButton) {
            GaugePreferences.restoreGeneralDefaults();
            this.returnToCurrentPrefs();
            fillFieldValues();
        }
    }

    private int getDefaultProjectorIndex(String proj) {
        if (proj.equalsIgnoreCase("Coordinate")) {
            return 2;
        } else if (proj.equalsIgnoreCase("PCA")) {
            return 1;
        } else if (proj.equalsIgnoreCase("Sammon")) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Populate fields with current data
     */
    public void fillFieldValues() {
        defaultProjector.setSelectedIndex(getDefaultProjectorIndex(theGaugePanel.getGauge().getDefaultProjector()));
        tolerance.setText(Double.toString(theGaugePanel.getGauge().getCurrentProjector().getTolerance()));
        perturbationFactor.setText(Double.toString(theGaugePanel.getGauge().getCurrentProjector().getPerturbationAmount()));

        int i = theGaugePanel.getGauge().getCurrentProjector().getAddMethodIndex();
        addMethod.setSelectedIndex(i);
    }

    /**
     * Set projector values based on fields
     */
    public void commit() {
        theGaugePanel.getGauge().setDefaultProjector(defaultProjector.getSelectedItem().toString());
        theGaugePanel.getGauge().getCurrentProjector().setTolerance(Double.valueOf(tolerance.getText()).doubleValue());
        theGaugePanel.getGauge().getCurrentProjector().setPerturbationAmount(Double.valueOf(perturbationFactor.getText())
                                                                             .doubleValue());
        theGaugePanel.getGauge().getCurrentProjector().setAddMethod(addMethod.getSelectedItem().toString());
    }

    /**
     * Restores the changed fields to their previous values Used when user cancels out of the dialog to undo whatever
     * changes were made in actionPerformed
     */
    public void returnToCurrentPrefs() {
        theGaugePanel.getGauge().setDefaultProjector(GaugePreferences.getDefaultProjector());
        theGaugePanel.getGauge().getCurrentProjector().setTolerance(GaugePreferences.getTolerance());
        theGaugePanel.getGauge().getCurrentProjector().setPerturbationAmount(GaugePreferences.getPerturbationAmount());
        theGaugePanel.getGauge().getCurrentProjector().setAddMethod(GaugePreferences.getAddMethod());
    }

    /**
     * Sets selected preferences as user defaults to be used each time program is launched Called when "ok" is pressed
     */
    public void setAsDefault() {
        GaugePreferences.setDefaultProjector(defaultProjector.getSelectedItem().toString());
        GaugePreferences.setTolerance(Double.parseDouble(tolerance.getText()));
        GaugePreferences.setPerturbationAmount(Double.parseDouble(perturbationFactor.getText()));
        GaugePreferences.setAddMethod(addMethod.getSelectedItem().toString());
    }
}
