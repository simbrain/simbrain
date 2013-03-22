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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;

/**
 * Implement a simple hearing sensor. When the phrase is heard, the sensor is
 * activated and and outputValue is sent out.
 *
 * @author Jeff Yoshimi
 *
 */
public class Hearing extends Sensor {

    /** The thing this hearing sensor listens for. */
    private String phrase = "";

    /** The heard phrase thought bubble image to be rendered */
    private BufferedImage image;

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
     * @param outputAmount the amount to output when this sensor is activated
     */
    public Hearing(OdorWorldEntity parent, String phrase, double outputAmount) {
        super(parent, "Hear: \"" + phrase + "\"");
        this.phrase = phrase;
        this.outputAmount = outputAmount;
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
    /**
     * @return the buffered image
     */
    public BufferedImage getImage() {
        image = new BufferedImage(80, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(OdorWorldResourceManager.getImage("ThoughtBubble.png"), 0, 0, null);
        g.setColor(Color.black);
        int fontSize = 20; // is there a more elegant way to change font size?
        if (phrase.length() <= 4) {
            fontSize = 15;
        } else if (phrase.length() <= 8) {
            fontSize = 10;
        } else {
            fontSize = 8;
        }
        g.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        FontMetrics fm = g.getFontMetrics(); // replace with toolkit?
        int x = (image.getWidth() - fm.stringWidth(phrase)) / 2;
        int y = (fm.getAscent() + ((image.getHeight() - (fm.getAscent() + fm.getDescent()))) / 2);
        g.drawString(phrase, x, y); // todo: fix bug: string appears before image. Put in OdorWorldRenderer instead?
        g.dispose();
        return image;
    }
}
