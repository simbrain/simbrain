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
package org.simbrain.world.odorworld;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;

import org.simbrain.util.SimbrainMath;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.effectors.Speech;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Hearing;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;
import org.simbrain.world.odorworld.sensors.TileSensor;

/**
 * The OdorWorldRenderer class draws an odor world on the screen. It draws all
 * entities and an optional background image.
 *
 * Adapted from Developing Games in Java, by David Brackeen.
 */
public class OdorWorldRenderer {

    /** Background image. */
    private Image background;

    /** Sensor color. */
    private static float sensorColor = Color.RGBtoHSB(255, 0, 0, null)[0];

    /** Sensor diameter. */
    private final static int SENSOR_DIAMATER = 6;

    /**
     * Sets the background to draw.
     */
    public void setBackground(Image background) {
        this.background = background;
    }

    /**
     * Draws the odor world.
     */
    public void draw(Graphics2D g, OdorWorld world, int screenWidth,
            int screenHeight) {

        // Draw white background, if needed
        if (background == null || screenHeight > background.getHeight(null)) {
            g.setColor(Color.white);
            g.fillRect(0, 0, screenWidth, screenHeight);
        }

        // Draw background image, if any
        if (background != null) {
            g.drawImage(background, 0, 0, null);
        }


        // For debugging world bounds
        // g.setColor(Color.black);
        // g.drawRect(0, 0, world.getWidth(), world.getHeight());

        // Draw entities
        for (OdorWorldEntity entity : world.getObjectList()) {
            int x = Math.round(entity.getX());
            int y = Math.round(entity.getY());
            // Below: Was thinking about some sort of indication that entities
            // collided.
            // if (sprite.hasCollided()) {
            // g.drawRect((int) sprite.getX(), (int)
            // sprite.getY(),sprite.getWidth(), sprite.getHeight());
            // }

            // For debugging entity boundary issues
            // g.drawRect((int) entity.getX(), (int) entity.getY(),
            // entity.getWidth(), entity.getHeight());

            while (g.drawImage(entity.getImage(), x, y, null) == false) {
                ; // keep trying to draw the image until you can. Dangerous?
            }

            // Display effector related graphics
            for (Effector effector : entity.getEffectors()) {
                if (effector instanceof Speech) {
                    if (((Speech) effector).isActivated()) {
                        g.drawImage(((Speech) effector).getImage(), null, x + entity.getWidth(), y - entity.getHeight());
                    }
                }
            }

            // Display sensor related graphics
            if (entity.isShowSensors()) {
                for (Sensor sensor : entity.getSensors()) {
                    if (sensor instanceof TileSensor) {
                        TileSensor tile = (TileSensor) sensor;
                        g.drawRect(tile.getX(), tile.getY(),
                                tile.getWidth(), tile.getHeight());

                    } else if (sensor instanceof Hearing) {
                        if (((Hearing) sensor).isActivated()) {
                            g.drawImage(((Hearing) sensor).getImage(), null, x + entity.getWidth(), y - entity.getHeight());
                        }
                    } else if (sensor instanceof SmellSensor) {

                        double val = SimbrainMath
                                .getVectorNorm(((SmellSensor) sensor)
                                        .getCurrentValue());
                        float saturation = 0;
                        if (world.getTotalSmellVectorLength() > 0) {
                            saturation = checkValid((float) Math.abs(val
                                    / (1 * world.getTotalSmellVectorLength())));
                        }
                        g.setPaint(Color.getHSBColor(sensorColor, saturation,
                                1));
                        // System.out.println(val + "--" +
                        // world.getTotalSmellVectorLength());
                        double[] location = ((SmellSensor) sensor)
                                .getLocation();
                        g.fillOval((int) location[0] - SENSOR_DIAMATER / 2,
                                (int) location[1] - SENSOR_DIAMATER / 2,
                                SENSOR_DIAMATER, SENSOR_DIAMATER);
                        g.setColor(Color.black);
                        g.drawOval((int) location[0] - SENSOR_DIAMATER / 2,
                                (int) location[1] - SENSOR_DIAMATER / 2,
                                SENSOR_DIAMATER, SENSOR_DIAMATER);
                    }
                }
            }
        }
    }

    /**
     * Check whether the specified saturation is valid or not.
     *
     * @param val the saturation value to check.
     * @return whether it is valid or not.
     */
    private float checkValid(final float val) {
        float tempval = val;

        if (val > 1) {
            tempval = 1;
        }

        if (val < 0) {
            tempval = 0;
        }

        return tempval;
    }

}
