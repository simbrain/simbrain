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

import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Implement a simple hearing sensor. When the phrase is heard, the sensor is
 * activated and and outputValue is sent out.
 *
 * @author Jeff Yoshimi
 *
 */
public class Hearing extends Sensor {

    /** The thing this hearing sensor listens for. */
    private String phrase = "Test";

    /**
     * Whether this is activated.
     */
    private boolean activated;

    /**
     * If amount to pass out if this sensor is activated.
     */
    private double outputAmount = 1;

    /**
     * Construct the hearing sensor.
     *
     * @param parent parent entity
     * @param phrase the phrase associated with this sensor
     */
    public Hearing(OdorWorldEntity parent, String phrase) {
        super(parent, "Hear: \"" + phrase + "\"");
        this.phrase = phrase;
    }

    @Override
    public void update() {
        activated = false;
        for (String heardPhrase : this.getParent().getCurrentlyHeardPhrases()) {
            if (phrase.equalsIgnoreCase(heardPhrase)) {
                activated = true;
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

}
