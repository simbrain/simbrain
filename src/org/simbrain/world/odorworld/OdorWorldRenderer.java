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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import org.simbrain.util.SimbrainMath;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.effectors.Speech;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;
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

    /** The speech balloon or thought bubble image to be rendered */
    private BufferedImage vocalize;

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
            //         entity.getWidth(), entity.getHeight());

            while (g.drawImage(entity.getImage(), x, y, null) == false) {
                ; // keep trying to draw the image until you can. Dangerous?
            }

            // Display effector related graphics
            for (Effector effector : entity.getEffectors()) {
                if (effector instanceof Speech) {
                    if (((Speech) effector).isActivated()) {
                        int imageX = x;
                        int imageY = y;
                        RotatingEntity rotatingEntity = (RotatingEntity) entity;

                        if (entity.getWidth() == 40) {
                            if (rotatingEntity.getHeading() < 7.5) {
                                imageX = imageX + entity.getWidth();
                                imageY = imageY - entity.getHeight();
                            } else if (rotatingEntity.getHeading() < 52.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 97.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 142.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 187.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 232.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 277.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() >= 277.5) {
                                // todo: fill in
                            }
                        } else if (rotatingEntity.getWidth() == 96) {
                            if (rotatingEntity.getHeading() < 7.5) {
                                imageX = x + entity.getWidth()*2/3;
                                imageY = y - entity.getHeight()/3;
                            } else if (rotatingEntity.getHeading() < 52.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 97.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 142.5) {

                            } else if (rotatingEntity.getHeading() < 187.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 232.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 277.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() >= 277.5) {
                                // todo: fill in
                            }
                        }
                        else if (entity.getWidth() == 128) {
                            if (rotatingEntity.getHeading() < 7.5) {
                                imageX = x + entity.getWidth()*3/4;
                                imageY = y - entity.getHeight()/8;
                            } else if (rotatingEntity.getHeading() < 52.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 97.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 142.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 187.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 232.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() < 277.5) {
                                // todo: fill in
                            } else if (rotatingEntity.getHeading() >= 277.5) {
                                // todo: fill in
                            }
                        }
                        g.drawImage(getSpeechBalloon((Speech) effector), null, imageX, imageY);
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
                            int imageX = x;
                            int imageY = y;
                            RotatingEntity rotatingEntity = (RotatingEntity) entity;

                            if (entity.getWidth() == 40) {
                                if (rotatingEntity.getHeading() < 7.5) {
                                    imageX = imageX + entity.getWidth()*3/4;
                                    imageY = imageY - entity.getHeight();
                                } else if (rotatingEntity.getHeading() < 52.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 97.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 142.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 187.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 232.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 277.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() >= 277.5) {
                                    // todo: fill in
                                }
                            } else if (rotatingEntity.getWidth() == 96) {
                                if (rotatingEntity.getHeading() < 7.5) {
                                    imageX = x + entity.getWidth()*2/3;
                                    imageY = y - entity.getHeight()/3;
                                } else if (rotatingEntity.getHeading() < 52.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 97.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 142.5) {

                                } else if (rotatingEntity.getHeading() < 187.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 232.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 277.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() >= 277.5) {
                                    // todo: fill in
                                }
                            }
                            else if (entity.getWidth() == 128) {
                                if (rotatingEntity.getHeading() < 7.5) {
                                    imageX = x + entity.getWidth()*3/4;
                                    imageY = y - entity.getHeight()/8;
                                } else if (rotatingEntity.getHeading() < 52.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 97.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 142.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 187.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 232.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() < 277.5) {
                                    // todo: fill in
                                } else if (rotatingEntity.getHeading() >= 277.5) {
                                    // todo: fill in
                                }
                            }
                            g.drawImage(getThoughtBubble((Hearing) sensor), null, imageX, imageY);
                            System.out.println("agent: " + entity.getName() + " at " + x + "," + y + " ; imageX = " + imageX + " ; imageY = " + imageY);
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
     * Draws the speech balloon.
     * @return the buffered image
     */
    public BufferedImage getSpeechBalloon(Speech effector) {
        vocalize = new BufferedImage(80, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = vocalize.createGraphics();
        g.drawImage(OdorWorldResourceManager.getImage("SpeechBalloon.png"), 0, 0, null);
        g.setColor(Color.black);
        int fontSize = 20; // is there a more elegant way to change font size?
        if (effector.getPhrase().length() <= 4) {
            fontSize = 15;
        } else if (effector.getPhrase().length() <= 8) {
            fontSize = 10;
        } else {
            fontSize = 8;
        }
        g.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        FontMetrics fm = g.getFontMetrics();
        int x = (vocalize.getWidth() - fm.stringWidth(effector.getPhrase())) / 2;
        int y = (fm.getAscent() + ((vocalize.getHeight() - (fm.getAscent() + fm.getDescent()))) / 2);
        g.drawString(effector.getPhrase(), x, y);
        g.dispose();
        return vocalize;
    }

    /**
     * Draws the thought bubble.
     * @return the buffered image
     */
    public BufferedImage getThoughtBubble(Hearing sensor) {
        vocalize = new BufferedImage(80, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = vocalize.createGraphics();
        g.drawImage(OdorWorldResourceManager.getImage("ThoughtBubble.png"), 0, 0, null);
        g.setColor(Color.black);
        int fontSize = 20; // is there a more elegant way to change font size?
        if (sensor.getPhrase().length() <= 4) {
            fontSize = 15;
        } else if (sensor.getPhrase().length() <= 8) {
            fontSize = 10;
        } else {
            fontSize = 8;
        }
        g.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        FontMetrics fm = g.getFontMetrics();
        int x = (vocalize.getWidth() - fm.stringWidth(sensor.getPhrase())) / 2;
        int y = (fm.getAscent() + ((vocalize.getHeight() - (fm.getAscent() + fm.getDescent()))) / 2);
        g.drawString(sensor.getPhrase(), x, y);
        g.dispose();
        return vocalize;
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
