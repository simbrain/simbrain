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
    /** Gauge panel for which settings are changed. */
    private Gauge gauge;
    /** Text field for setting pertubation factor. */
    private JTextField perturbationFactor = new JTextField();
    /** Text field for setting value of tolerance. */
    private JTextField tolerance = new JTextField();
    /** Methods for adding new datapoints. */
    private JComboBox addMethod = new JComboBox(Settings.getAddMethods());
    /** Projector to be used with every new gauge. */
    private JComboBox defaultProjector = new JComboBox(Gauge.getProjectorList());
    /** Restores gauge preferencs defaults. */
    private JButton defaultButton = new JButton("Restore defaults");
    /** Panel to add and organize content. */
    private LabelledItemPanel myContentPane = new LabelledItemPanel();

    /**
     * This method is the default constructor.
     * @param gp Gauge panel to open dialog for
     */
    public DialogGeneral(final Gauge gauge) {
        this.gauge = gauge;
        init();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        setTitle("General Dialog");

        fillFieldValues();
        myContentPane.setBorder(BorderFactory.createEtchedBorder());

        myContentPane.addItem("Tolerance", tolerance);
        myContentPane.addItem("Perturbation factor", perturbationFactor);
        myContentPane.addItem("Add new datapoints using", addMethod);
        myContentPane.addItem("Default projector", defaultProjector);

        defaultButton.addActionListener(this);
        addButton(defaultButton);
        setContentPane(myContentPane);
    }

    /**
     * @param e Responds to button pressing events
     */
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == defaultButton) {
            GaugePreferences.restoreGeneralDefaults();
            this.returnToCurrentPrefs();
            fillFieldValues();
        }
    }

    /**
     * @param proj Name of default projector.
     * @return number projector corresponds to in combo box index
     */
    private int getDefaultProjectorIndex(final String proj) {
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
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        defaultProjector.setSelectedIndex(getDefaultProjectorIndex(gauge.getDefaultProjector()));
        tolerance.setText(Double.toString(gauge.getCurrentProjector().getTolerance()));
        perturbationFactor.setText(Double.toString(gauge
                .getCurrentProjector().getPerturbationAmount()));

        int i = gauge.getCurrentProjector().getAddMethodIndex();
        addMethod.setSelectedIndex(i);
    }

    /**
     * Set projector values based on fields.
     */
    public void commit() {
        gauge.setDefaultProjector(defaultProjector.getSelectedItem().toString());
        gauge.getCurrentProjector().setTolerance(Double.valueOf(tolerance.getText()).doubleValue());
        gauge.getCurrentProjector()
        .setPerturbationAmount(Double.valueOf(perturbationFactor.getText()).doubleValue());
        gauge.getCurrentProjector().setAddMethod(addMethod.getSelectedItem().toString());
    }

    /**
     * Restores the changed fields to their previous values Used when user cancels out of the dialog to undo whatever
     * changes were made in actionPerformed.
     */
    public void returnToCurrentPrefs() {
        gauge.setDefaultProjector(GaugePreferences.getDefaultProjector());
        gauge.getCurrentProjector().setTolerance(GaugePreferences.getTolerance());
        gauge.getCurrentProjector().setPerturbationAmount(GaugePreferences.getPerturbationAmount());
        gauge.getCurrentProjector().setAddMethod(GaugePreferences.getAddMethod());
    }

    /**
     * Sets selected preferences as user defaults to be used each time program is launched Called when "ok" is pressed.
     */
    public void setAsDefault() {
        GaugePreferences.setDefaultProjector(defaultProjector.getSelectedItem().toString());
        GaugePreferences.setTolerance(Double.parseDouble(tolerance.getText()));
        GaugePreferences.setPerturbationAmount(Double.parseDouble(perturbationFactor.getText()));
        GaugePreferences.setAddMethod(addMethod.getSelectedItem().toString());
    }
}
