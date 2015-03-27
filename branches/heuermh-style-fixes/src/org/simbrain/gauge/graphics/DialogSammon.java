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

import org.simbrain.gauge.GaugePreferences;
import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.core.ProjectSammon;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextField;


/**
 * <b>DialogSammon</b> is a dialog box for setting the properties of the  Sammon mapping algorithm.
 */
public class DialogSammon extends StandardDialog implements ActionListener {
    private Gauge theGauge;
    private JTextField epsilonField = new JTextField();
    private JButton defaultButton = new JButton("Restore defaults");
    private LabelledItemPanel myContentPane = new LabelledItemPanel();

    /**
     * This method is the default constructor.
     */
    public DialogSammon(Gauge gauge) {
        theGauge = gauge;
        init();
    }

    /**
     * This method initialises the components on the panel.
     */
    private void init() {
        setTitle("Sammon Dialog");

        fillFieldValues();
        myContentPane.setBorder(BorderFactory.createEtchedBorder());
        epsilonField.setColumns(3);
        myContentPane.addItem("Step size", epsilonField);

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
            GaugePreferences.restoreSammonDefaults();
            this.returnToCurrentPrefs();
            fillFieldValues();
        }
    }

    /**
     * Populate fields with current data
     */
    public void fillFieldValues() {
        ProjectSammon gauge = (ProjectSammon) theGauge.getCurrentProjector();
        epsilonField.setText(Double.toString(gauge.getEpsilon()));
    }

    /**
     * Set projector values based on fields
     */
    public void commit() {
        ((ProjectSammon) theGauge.getCurrentProjector()).setEpsilon(Double.valueOf(epsilonField.getText()).doubleValue());
    }

    /**
     * Restores the changed fields to their previous values Used when user cancels out of the dialog to undo whatever
     * changes were made in actionPerformed
     */
    public void returnToCurrentPrefs() {
        ((ProjectSammon) theGauge.getCurrentProjector()).setEpsilon(GaugePreferences.getEpsilon());
    }

    /**
     * Sets selected preferences as user defaults to be used each time program is launched Called when "ok" is pressed
     */
    public void setAsDefault() {
        GaugePreferences.setEpsilon(Double.parseDouble(epsilonField.getText()));
    }
}