/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
import javax.swing.JTextField;

import org.simbrain.gauge.GaugePreferences;
import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.core.ProjectSammon;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;


/**
 * <b>DialogSammon</b> is a dialog box for setting the properties of the  Sammon mapping algorithm.
 */
public class DialogSammon extends StandardDialog implements ActionListener {
    /** Gauge for which settings are changed. */
    private Gauge theGauge;
    /** Text field for showing and setting value of epsilon. */
    private JTextField epsilonField = new JTextField();
    /** Restores built-in defaults. */
    private JButton defaultButton = new JButton("Restore defaults");
    /** Panel and layout for dialog.*/
    private LabelledItemPanel myContentPane = new LabelledItemPanel();

    /**
     * This method is the default constructor.
     * @param gauge Settings of frame to be set
     */
    public DialogSammon(final Gauge gauge) {
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
     * @param e Respond to button pressing events.
     */
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == defaultButton) {
            GaugePreferences.restoreSammonDefaults();
            this.returnToCurrentPrefs();
            fillFieldValues();
        }
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        ProjectSammon gauge = (ProjectSammon) theGauge.getCurrentProjector();
        epsilonField.setText(Double.toString(gauge.getEpsilon()));
    }

    /**
     * Set projector values based on fields.
     */
    public void commit() {
        ((ProjectSammon) theGauge.getCurrentProjector())
        .setEpsilon(Double.valueOf(epsilonField.getText()).doubleValue());
    }

    /**
     * Restores the changed fields to their previous values Used when user cancels out of the dialog to undo whatever
     * changes were made in actionPerformed.
     */
    public void returnToCurrentPrefs() {
        ((ProjectSammon) theGauge.getCurrentProjector()).setEpsilon(GaugePreferences.getEpsilon());
    }

    /**
     * Sets selected preferences as user defaults to be used each time program is launched Called when "ok" is pressed.
     */
    public void setAsDefault() {
        GaugePreferences.setEpsilon(Double.parseDouble(epsilonField.getText()));
    }
}
