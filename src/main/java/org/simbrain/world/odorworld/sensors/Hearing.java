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
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

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
            order = 3)
    private String phrase = DEFAULT_PHRASE;

    /**
     * Maximum characters per row before warping around in a HearingNode.
     */
    @UserParameter(label = "Characters per Row",
            description = "The maximum number of characters that can be displayed in one row in the hearing bubble. "
                    + "This setting only affects visual representation.", order = 4)
    private int charactersPerRow = 32;

    /**
     * Whether this is activated.
     */
    private boolean activated = false;

    /**
     * If amount to pass out if this sensor is activated.
     */
    @UserParameter(label = "Output Amount",
            description = "The amount of activation to be sent to a neuron coupled with this sensor.",
            order = 5)
    private double outputAmount = DEFAULT_OUTPUT_AMOUNT;

    //TODO: Clean up / Make this settable
    private int time = 0;
    private int lingerTime = 10;

    /**
     * Construct the hearing sensor.
     *
     * @param phrase       the phrase associated with this sensor
     * @param outputAmount the amount to output when this sensor is activated
     */
    public Hearing(String phrase, double outputAmount) {
        super("Hear: \"" + phrase + "\"");
        this.phrase = phrase;
        this.outputAmount = outputAmount;
    }

    /**
     * Construct the hearing sensor.
     */
    public Hearing() {
        super("Hear: \"" + DEFAULT_PHRASE + "\"");
    }

    public Hearing(Hearing hearing) {
        super(hearing);
        this.phrase = hearing.phrase;
        this.charactersPerRow = hearing.charactersPerRow;
    }

    @Override
    public void update(OdorWorldEntity parent) {

        // TODO: Only hear if in range of speaker.  Will need some new range variable.

        for (String heardPhrase : parent.getCurrentlyHeardPhrases()) {
            if (phrase.equalsIgnoreCase(heardPhrase)) {
                if (!activated) {
                    activated = true;
                    getEvents().fireUpdated();
                }
                time = lingerTime;
            }
        }
        time--;
        if (!(time > 0)) {
            if (activated) {
                activated = false;
                getEvents().fireUpdated();
            }
        }
    }

    public String getPhrase() {
        return phrase;
    }

    @Consumable(customDescriptionMethod = "getAttributeDescription")
    public void setPhrase(String phrase) {
        this.phrase = phrase;
        getEvents().firePropertyChanged();
    }

    public boolean isActivated() {
        return activated;
    }

    @Producible(customDescriptionMethod = "getAttributeDescription")
    public double getValue() {
        if (activated) {
            return outputAmount;
        } else {
            return 0;
        }
    }

    @Override
    public String getLabel() {
        if (super.getLabel().isEmpty()) {
            return "Hear " + phrase;
        } else {
            return super.getLabel();
        }
    }


    @Override
    public String getName() {
        return "Hearing Sensor";
    }

    @Override
    public Hearing copy() {
        return new Hearing(this);
    }

    public int getCharactersPerRow() {
        return charactersPerRow;
    }

    public void setCharactersPerRow(int charactersPerRow) {
        this.charactersPerRow = charactersPerRow;
    }

}
