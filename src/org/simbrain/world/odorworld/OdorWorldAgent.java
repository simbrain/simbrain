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
package org.simbrain.world.odorworld;

import java.awt.Point;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SimbrainMath;
import org.simbrain.world.Agent;
import org.simbrain.world.World;


/**
 * <b>Agent</b> represents in a creature in the world which can react to stimuli and move.  Agents are controlled by
 * neural networks, in particular their input and output nodes.
 */
public class OdorWorldAgent extends OdorWorldEntity implements Agent {
    private final double initWhiskerLength = 23;
    private final double four = 4;
    private double whiskerAngle = Math.PI / four; // angle in radians
    private double whiskerLength = initWhiskerLength;
    private double turnIncrement = 1;
    private double movementIncrement = 2;

    private final double initOri = 300;
    /** orientation of this object; used only by creature currently. */
    private double orientation = initOri;

    public OdorWorldAgent() {
    }

    public OdorWorldAgent(final OdorWorld wr, final String nm,
            final String type, final int x, final int y, final double ori) {
        super(wr, type, x, y);
        super.setName(nm);
        setOrientation(ori);
    }

    /**
     * @return orientation in degrees
     */
    public double getOrientation() {
        return orientation;
    }

    /**
     * @return orientation in degrees
     */
    public double getOrientationRad() {
        final int deg = 180;
        return (orientation * Math.PI) / deg;
    }

    /**
     * Set the orienation of the creature.
     *
     * @param d the orientation, in degrees
     */
    public void setOrientation(final double d) {
        orientation = d;
        final double start = 7.5;
        final double inc = 15;
        final double oneUnit = 1;
        final double twoUnits = 2;
        final double threeUnits = 3;
        final double fourUnits = 4;
        final double fiveUnits = 5;
        final double sixUnits = 6;
        final double sevenUnits = 7;
        final double eightUnits = 8;
        final double nineUnits = 9;
        final double tenUnits = 10;
        final double elevenUnits = 11;
        final double twelveUnits = 12;
        final double thirteenUnits = 13;
        final double fourteenUnits = 14;
        final double fifteenUnits = 15;
        final double sixteenUnits = 16;
        final double seventeenUnits = 17;
        final double eighteenUnits = 18;
        final double nineteenUnits = 19;
        final double twentyUnits = 20;
        final double twentyOneUnits = 21;
        final double twentyTwoUnits = 22;
        final double twentyThreeUnits = 23;

        if ((d <= start + twentyThreeUnits * inc) && (d < start)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_0.gif"));
        } else if ((d >= start) && (d < start + oneUnit * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_15.gif"));
        } else if ((d >= start + oneUnit * inc) && (d < start + twoUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_30.gif"));
        } else if ((d >= start + twoUnits * inc) && (d < start + threeUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_45.gif"));
        } else if ((d >= start + threeUnits * inc) && (d < start + fourUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_60.gif"));
        } else if ((d >= start + fourUnits * inc) && (d < start + fiveUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_75.gif"));
        } else if ((d >= start + fiveUnits * inc) && (d < start + sixUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_90.gif"));
        } else if ((d >= start + sixUnits * inc) && (d < start + sevenUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_105.gif"));
        } else if ((d >= start + sevenUnits * inc) && (d < start + eightUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_120.gif"));
        } else if ((d >= start + eightUnits * inc) && (d < start + nineUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_135.gif"));
        } else if ((d >= start + nineUnits * inc) && (d < start + tenUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_150.gif"));
        } else if ((d >= start + tenUnits * inc) && (d < start + elevenUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_165.gif"));
        } else if ((d >= start + elevenUnits * inc) && (d < start + twelveUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_180.gif"));
        } else if ((d >= start + twelveUnits * inc) && (d < start + thirteenUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_195.gif"));
        } else if ((d >= start + thirteenUnits * inc) && (d < start + fourteenUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_210.gif"));
        } else if ((d >= start + fourteenUnits * inc) && (d < start + fifteenUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_225.gif"));
        } else if ((d >= start + fifteenUnits * inc) && (d < start + sixteenUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_240.gif"));
        } else if ((d >= start + sixteenUnits * inc) && (d < start + seventeenUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_255.gif"));
        } else if ((d >= start + seventeenUnits * inc) && (d < start + eighteenUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_270.gif"));
        } else if ((d >= start + eighteenUnits * inc) && (d < start + nineteenUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_285.gif"));
        } else if ((d >= start + nineteenUnits * inc) && (d < start + twentyUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_300.gif"));
        } else if ((d >= start + twentyUnits * inc) && (d < start + twentyOneUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_315.gif"));
        } else if ((d >= start + twentyOneUnits * inc) && (d < start + twentyTwoUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_330.gif"));
        } else if ((d >= start + twentyTwoUnits * inc) && (d < start + twentyThreeUnits * inc)) {
            getTheImage().setImage(ResourceManager.getImage("Mouse_345.gif"));
        }
    }

    /**
     * @return position of left whisker, given orientation of creature
     */
    public Point getLeftWhisker() {
        double theta = getOrientationRad();
        int x = (int) (getLocation().x + (whiskerLength * Math.cos(theta + whiskerAngle)));
        int y = (int) (getLocation().y - (whiskerLength * Math.sin(theta + whiskerAngle)));

        return new Point(x, y);
    }

    /**
     * @return position of right whisker, given orientation of creature
     */
    public Point getRightWhisker() {
        double theta = getOrientationRad();
        int x = (int) (getLocation().x + (whiskerLength * Math.cos(theta - whiskerAngle)));
        int y = (int) (getLocation().y - (whiskerLength * Math.sin(theta - whiskerAngle)));

        return new Point(x, y);
    }

    public void turnRight(final double value) {
        double temp = computeAngle(getOrientation() - (value * turnIncrement));
        setOrientation(temp);

        //System.out.println("Orientation = " + getOrientation());
    }

    public void turnLeft(final double value) {
        double temp = computeAngle(getOrientation() + (value * turnIncrement));
        setOrientation(temp);

        //System.out.println("Orientation = " + getOrientation());
    }

    /**
     * Ensures that val lies between 0 and 360.
     * @param val the value to compute
     * @return val's "absolute angle"
     */
    private double computeAngle(final double val) {
        final int circleDeg = 360;
        double temp = val;
        while (val >= circleDeg) {
            temp -= circleDeg;
        }

        while (val < 0) {
            temp += circleDeg;
        }

        return temp;
    }

    public void goStraightForward(final double value) {
        if (value == 0) {
            return;
        }
        double temp = value;

        double theta = getOrientationRad();
        temp *= movementIncrement;

        Point p = new Point(
                            (int) (Math.round(getLocation().x + (temp * Math.cos(theta)))),
                            (int) (Math.round(getLocation().y - (temp * Math.sin(theta)))));

        if (validMove(p)) {
            moveTo(0, p.x, p.y);
            wrapAround();
        }
    }

    public void goStraightBackward(final double value) {
        if (value == 0) {
            return;
        }
        double temp = value;

        double theta = getOrientationRad();
        temp *= movementIncrement;

        Point p = new Point(
                            (int) (Math.round(getLocation().x - (temp * Math.cos(theta)))),
                            (int) (Math.round(getLocation().y + (temp * Math.sin(theta)))));

        if (validMove(p)) {
            moveTo(0, p.x, p.y);
            wrapAround();
        }
    }

    /**
     * Check to see if the creature can move to a given new location.  If it is off screen or on top of a creature,
     * disallow the move.
     *
     * @param possibleCreatureLocation on-screen location to be checked
     *
     * @return true if the move is valid, false otherwise
     */
    protected boolean validMove(final Point possibleCreatureLocation) {
        if ((this.getParent().getUseLocalBounds()) && !this.getParent().contains(possibleCreatureLocation)) {
            return false;
        }

        if (!this.getParent().getObjectInhibitsMovement()) {
            return true;
        }

        //creature collision
        for (int i = 0; i < this.getParent().getAbstractEntityList().size(); i++) {
            AbstractEntity temp = (AbstractEntity) this.getParent().getAbstractEntityList().get(i);

            if (temp == this) {
                continue;
            }

            if (temp.getRectangle().intersects(getRectangle(possibleCreatureLocation))) {
                if (temp.getEdible()) {
                    temp.setBites(temp.getBites() + 1);

                    if (temp.getBites() >= temp.getBitesToDie()) {
                        temp.terminate();
                    }
                }

                return false;
            }
        }

        return true;
    }

    /**
     * Implements a "video-game" world or torus, such that when an object leaves on side of the screen it reappears on
     * the other.
     */
    public void wrapAround() {
        if (this.getParent().getUseLocalBounds()) {
            return;
        }

        if (getLocation().x >= this.getParent().getWorldWidth()) {
            getLocation().x -= this.getParent().getWorldWidth();
        }

        if (getLocation().x < 0) {
            getLocation().x += this.getParent().getWorldWidth();
        }

        if (getLocation().y >= this.getParent().getWorldHeight()) {
            getLocation().y -= this.getParent().getWorldHeight();
        }

        if (getLocation().y < 0) {
            getLocation().y += this.getParent().getWorldHeight();
        }
    }

    /**
     * Actiate a motor command on this agent.
     *
     * @param commandList the command itself
     * @param value the activation level of the output neuron which produced this command
     */
    public void setMotorCommand(final String[] commandList, final double value) {
        String name = commandList[0];

        if (name.equals("Forward")) {
            goStraightForward(value);
        } else if (name.equals("Backward")) {
            goStraightBackward(value);
        } else if (name.equals("Left")) {
            turnLeft(value);
        } else if (name.equals("Right")) {
            turnRight(value);
        } else {
            absoluteMovement(name, value);
        }

        this.getParent().repaint();
    }

    /**
     * Move the agent in an absolute direction.
     *
     * @param name the name of the direction to move in
     * @param value activation level of associated output node
     */
    private void absoluteMovement(final String name, final double value) {
        Point creaturePosition = getLocation();
        int possiblePositionX = getLocation().x;
        int possiblePositionY = getLocation().y;

        int increment = (int) (movementIncrement * value);

        if (name.equals("North")) {
            possiblePositionY = creaturePosition.y - increment;
        } else if (name.equals("South")) {
            possiblePositionY = creaturePosition.y + increment;
        } else if (name.equals("West")) {
            possiblePositionX = creaturePosition.x - increment;
        } else if (name.equals("East")) {
            possiblePositionX = creaturePosition.x + increment;
        } else if (name.equals("North-west")) {
            possiblePositionX = creaturePosition.x - increment;
            possiblePositionY = creaturePosition.y - increment;
        } else if (name.equals("North-east")) {
            possiblePositionX = creaturePosition.x + increment;
            possiblePositionY = creaturePosition.y - increment;
        } else if (name.equals("South-west")) {
            possiblePositionX = creaturePosition.x - increment;
            possiblePositionY = creaturePosition.y + increment;
        } else if (name.equals("South-east")) {
            possiblePositionX = creaturePosition.x + increment;
            possiblePositionY = creaturePosition.y + increment;
        }

        Point possiblePosition = new Point(possiblePositionX, possiblePositionY);

        if (validMove(possiblePosition)) {
            setLocation(possiblePosition);
            wrapAround();
        }
    }

    //TODO: Do this operation for just the stim_id requested.  Make more efficient: Talk to Scott.

    /**
     * Get the stimulus associated with the a given sensory id.
     * @param sensorID the string representing the id
     * @return the stimulus for the id
     */
    public double getStimulus(final String[] sensorID) {
        int max = this.getParent().getHighestDimensionalStimulus();
        double[] currentStimulus = SimbrainMath.zeroVector(max);
        AbstractEntity temp = null;
        double distance = 0;

        String sensorLocation = sensorID[0];
        int sensorIndex = Integer.parseInt(sensorID[1]) - 1;

        //Sum proximal stimuli corresponding to each object
        if (sensorLocation.equals("Center")) {
            for (int i = 0; i < this.getParent().getAbstractEntityList().size(); i++) {
                temp = (AbstractEntity) this.getParent().getAbstractEntityList().get(i);
                distance = SimbrainMath.distance(temp.getLocation(), getLocation());

                if (temp == this) {
                    continue;
                }

                currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
            }
        } else if (sensorLocation.equals("Left")) {
            for (int i = 0; i < this.getParent().getAbstractEntityList().size(); i++) {
                temp = (AbstractEntity) this.getParent().getAbstractEntityList().get(i);
                distance = SimbrainMath.distance(temp.getLocation(), getLeftWhisker());

                if (temp == this) {
                    continue;
                }

                currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
            }
        } else if (sensorLocation.equals("Right")) {
            for (int i = 0; i < this.getParent().getAbstractEntityList().size(); i++) {
                temp = (AbstractEntity) this.getParent().getAbstractEntityList().get(i);
                distance = SimbrainMath.distance(temp.getLocation(), getRightWhisker());

                if (temp == this) {
                    continue;
                }

                currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
            }
        }

        return currentStimulus[sensorIndex % max];
    }

    /**
     * @return Returns the straight_factor.
     */
    public double getMovementIncrement() {
        return movementIncrement;
    }

    /**
     * @param straightFactor The straight_factor to set.
     */
    public void setMovementIncrement(final double straightFactor) {
        this.movementIncrement = straightFactor;
    }

    /**
     * @return Returns the turn_factor.
     */
    public double getTurnIncrement() {
        return turnIncrement;
    }

    /**
     * @param turnFactor The turnFactor to set.
     */
    public void setTurnIncrement(final double turnFactor) {
        this.turnIncrement = turnFactor;
    }

    /**
     * @return Returns the whiskerAngle.
     */
    public double getWhiskerAngle() {
        return whiskerAngle;
    }

    /**
     * @param whiskerAngle The whiskerAngle to set.
     */
    public void setWhiskerAngle(final double whiskerAngle) {
        this.whiskerAngle = whiskerAngle;
    }

    /**
     * @return Returns the whiskerLength.
     */
    public double getWhiskerLength() {
        return whiskerLength;
    }

    /**
     * @param whiskerLength The whiskerLength to set.
     */
    public void setWhiskerLength(final double whiskerLength) {
        this.whiskerLength = whiskerLength;
    }

    /**
     * @return this.getParent() world
     */
    public World getParentWorld() {
        return this.getParent();
    }
}
