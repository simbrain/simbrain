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
package org.simbrain.world.odorworld.effectors;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Model simple speech behaviors. Each speech effector is associated with a
 * specific phrase. To activate speech either manually activate or de-activate
 * it or set the "value" to a number above a threshold (currently set to 0).
 *
 * @author Jeff Yoshimi
 */
public class Speech extends Effector {

    //TODO: Possibly add a radius of influence
    //      Possibly add a threshold for the value above which to "speak"
    //      Possibly encapsulate phrase String in an utterance class 

    /** The thing this speech effector says. */
    private String phrase = "Test";

    /**
     * Whether this is activated. If so, display the phrase and notify all
     * hearing sensors.
     */
    private boolean activated;

    /** Threshold above which to "the message. */
    private double threshold = 0;

    /**
     * If amount is greater than threshold, activate the speech.
     */
    private double amount;

    /**
     * Construct the speech effector.
     *
     * @param parent parent entity
     * @param phrase the phrase associated with this effector
     */
    public Speech(OdorWorldEntity parent, String phrase) {
        super(parent, "Say: \"" + phrase + "\"");
        this.phrase = phrase;
    }

    @Override
    public void update() {
        if (amount > threshold) {
            activated = true;
            amount = 0; // reset
        } else {
            activated = false;
        }
        if (activated) {
            for (OdorWorldEntity entity : parent.getParentWorld()
                    .getObjectList()) {

                //TODO: Can add radius check here later

                // Don't talk to yourself
                if (entity != parent) {
                    entity.speakToEntity(phrase);
                }
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
     * @param activated the activated to set
     */
    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    /**
     * @return the amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * @param amount the amount to set
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

}
