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
package org.simbrain.world.odorworld.dialogs;

import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Hearing;

/**
 * Panel to add a hearing sensor to an entity.
 *
 * @author Lam Nguyen
 *
 */
public class HearingSensorPanel extends AbstractSensorPanel {

    /** Text field to edit phrase this sensor listens for. */
    private JTextField phrase = new JTextField();

    /** Texxt field to edit output amount */
    private JTextField outputAmount = new JTextField();

    /** Entity to which a hearing sensor is being added. */
    private OdorWorldEntity entity;

    /** Reference to hearing sensor. Initially null if this is a creation panel. */
    private Hearing hearingSensor;

    /** If true this is a creation panel. Otherwise it is an edit panel. */
    private boolean isCreationPanel;

    /**
     * Constructor for the case where a sensor is being created.
     *
     * @param entity the entity to which a hearing sensor is added.
     */
    public HearingSensorPanel(OdorWorldEntity entity) {
        this.entity = entity;
        isCreationPanel = true;
        addItem("Utterance", phrase);
        addItem("Output Amount", outputAmount);
        fillFieldValues();
    }

    /**
     * Constructor for the case where a sensor is being edited.
     *
     * @param entity parent entity
     * @param sensor sensor to edit
     */
    public HearingSensorPanel(OdorWorldEntity entity, Hearing sensor) {
        this.entity = entity;
        this.hearingSensor = sensor;
        isCreationPanel = false;
        addItem("Utterance", phrase);
        addItem("Output Amount", outputAmount);
        fillFieldValues();
    }

    @Override
    public void commitChanges() {
        if (isCreationPanel) {
            entity.addSensor(new Hearing((entity), phrase.getText(), Double
                    .parseDouble(outputAmount.getText())));
            if (phrase.getText().length() > 10) {
                checkPhrase();
            }
        } else {
            hearingSensor.setPhrase(phrase.getText());
            hearingSensor.setLabel("Hear: \"" + phrase.getText() + "\"");
            hearingSensor.setOutputAmount(Double.parseDouble(outputAmount
                    .getText()));
            hearingSensor.getParent().getParentWorld()
                    .fireEntityChanged(hearingSensor.getParent());
            if (phrase.getText().length() > 10) {
                checkPhrase();
            }
        }
    }

    /** 
     * Set the current values of all fields.
     */
    private void fillFieldValues() {
        if (isCreationPanel) {
            phrase.setText("" + Hearing.DEFAULT_PHRASE);
            outputAmount.setText("" + Hearing.DEFAULT_OUTPUT_AMOUNT);
        } else {
            phrase.setText("" + hearingSensor.getPhrase());
            outputAmount.setText("" + hearingSensor.getOutputAmount());
        }
    }

    /** Displays message when utterance is above 10 char. */
    private void checkPhrase() {
        JOptionPane
                .showOptionDialog(
                        null,
                        "Heard utterance is greater than 10 chars! Not guaranteed to render correctly.",
                        "Warning", JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE, null, null, null);
    }
}
