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
package org.simbrain.world.odorworld.sensors;

import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.gui.EntityAttributeNode;
import org.simbrain.world.odorworld.gui.HearingNode;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Implement a simple hearing sensor. When the phrase is heard, the sensor is
 * activated and and outputValue is sent out.
 *
 * @author Jeff Yoshimi
 */
public class Hearing extends Sensor implements VisualizableEntityAttribute {

    /**
     * Default phrase.
     */
    public static final String DEFAULT_PHRASE = "Hi!";

    /**
     * Default output amount.
     */
    public static final double DEFAULT_OUTPUT_AMOUNT = 1;

    /**
     * The thing this hearing sensor listens for.
     */
    @UserParameter(label = "Utterance",
            description = "The string or phrase associated with this sensor. Hearing sensors get activated "
                    + "when it senses a speech effectors of the same utterance.",
            defaultValue = DEFAULT_PHRASE,
            order = 3)
    private String phrase = DEFAULT_PHRASE;

    /**
     * Whether this is activated.
     */
    private boolean activated = false;

    /**
     * If amount to pass out if this sensor is activated.
     */
    @UserParameter(label = "Output Amount",
            description = "The amount of activation to be sent to a neuron coupled with this sensor.",
            defaultValue = "" + DEFAULT_OUTPUT_AMOUNT, order = 4)
    private double outputAmount = DEFAULT_OUTPUT_AMOUNT;

    /**
     * Construct the hearing sensor.
     *
     * @param parent       parent entity
     * @param phrase       the phrase associated with this sensor
     * @param outputAmount the amount to output when this sensor is activated
     */
    public Hearing(OdorWorldEntity parent, String phrase, double outputAmount) {
        super(parent, "Hear: \"" + phrase + "\"");
        this.phrase = phrase;
        this.outputAmount = outputAmount;
    }

    /**
     * Construct the hearing sensor.
     *
     * @param parent       parent entity
     */
    public Hearing(OdorWorldEntity parent) {
        super(parent, "Hear: \"" + DEFAULT_PHRASE + "\"");
    }

    //TODO: Clean up / Make this settable
    private int time = 0;
    private int lingerTime = 10;

    /**
     * Support for property change events.
     */
    protected transient PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    @Override
    public void update() {
        for (String heardPhrase : this.getParent().getCurrentlyHeardPhrases()) {
            if (phrase.equalsIgnoreCase(heardPhrase)) {
                if (!activated) {
                    activated = true;
                    changeSupport.firePropertyChange("activationChanged", null, true);
                }
                time = lingerTime;
            }
        }
        time--;
        if (!(time > 0)) {
            if (activated) {
                activated = false;
                changeSupport.firePropertyChange("activationChanged", null, false);
            }
        }
    }

    /**
     * @return the phrase
     */
    public String getPhrase() {
        return phrase;
    }

    /**
     * @param phrase the phrase to set
     */
    public void setPhrase(String phrase) {
        changeSupport.firePropertyChange("phraseChanged", null, null);
        this.phrase = phrase;
    }

    /**
     * @return the activated
     */
    public boolean isActivated() {
        return activated;
    }

    /**
     * @return the amount
     */
    public double getOutputAmount() {
        return outputAmount;
    }

    /**
     * @param amount the amount to set
     */
    public void setOutputAmount(double amount) {
        this.outputAmount = amount;
    }

    /**
     * @return the value
     */
    public double getValue() {
        if (activated) {
            return outputAmount;
        } else {
            return 0;
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public String getTypeDescription() {
        return "Hearing";
    }

    @Override
    public EditableObject copy() {
        return new Hearing(parent, phrase, outputAmount);
    }

    @Override
    public EntityAttributeNode getNode() {
        return new HearingNode(this);
    }
}
