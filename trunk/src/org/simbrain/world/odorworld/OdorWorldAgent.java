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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SimbrainMath;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;

/**
 * <b>Agent</b> represents in a creature in the world which can react to stimuli and move.  Agents are controlled by
 * neural networks, in particular their input and output nodes.
 */
public class OdorWorldAgent extends OdorWorldEntity implements Producer, Consumer {

    /** Initial length of mouse whisker. */
    private final double initWhiskerLength = 23;

    /** Angle of whisker in radians. */
    private static double WHISKER_ANGLE = Math.PI / 4;

    /** Lenght of whisker. */
    private double whiskerLength = initWhiskerLength;

    /** Amount agent should turn. */
    private double turnIncrement = 1;

    /** How fast agent should move. */
    private double movementIncrement = 2;

    /** Degrees in a circle. */
    private final static int DEGREES_IN_A_CIRCLE = 360;

    /** Initial orientation of agent. */
    private final double initOri = 300;

    /** Orientation of this object; used only by creature currently. */
    private double orientation = initOri;

    /** List of things this agent can do. */
    private ArrayList<ConsumingAttribute> effectorList = new ArrayList<ConsumingAttribute>();

    /** Default effector. */
    private ConsumingAttribute defaultEffector;

    /** List of sensors. */
    private ArrayList<ProducingAttribute> sensorList = new ArrayList<ProducingAttribute>();

    /** Default sensor. */
    private ProducingAttribute defaultSensor;

    /**
     * Default constructor.
     */
    public OdorWorldAgent() {
        initEffectorsAndSensors();
    }

    /**
     * Creates an instance of an agent.
     *
     * @param wr Odor world to place agent
     * @param nm Name of agent
     * @param type Type of agent
     * @param x Position
     * @param y Position
     * @param ori Orientation
     */
    public OdorWorldAgent(final OdorWorld wr, final String nm,
            final String type, final int x, final int y, final double ori) {
        super(wr, type, x, y);
        super.setName(nm);
        setOrientation(ori);
    }
/**
     * Initialize effectors and sensors, thereby allowing other Simbrain components to couple to the agent.
     */
    public void initEffectorsAndSensors() {
        defaultEffector = new Effector(this,"Forward");
        effectorList.add(defaultEffector);
        effectorList.add(new Effector(this, "Right"));
        effectorList.add(new Effector(this, "Left"));
        effectorList.add(new Effector(this, "North"));
        effectorList.add(new Effector(this, "South"));
        effectorList.add(new Effector(this, "East"));
        effectorList.add(new Effector(this, "West"));
        defaultSensor = new Sensor(this, "Center", 1);
        sensorList.add(defaultSensor);
        sensorList.add(new Sensor(this, "Center", 2));
        sensorList.add(new Sensor(this, "Center", 3));
        sensorList.add(new Sensor(this, "Center", 4));
        sensorList.add(new Sensor(this, "Right", 1));
        sensorList.add(new Sensor(this, "Right", 2));
        sensorList.add(new Sensor(this, "Right", 3));
        sensorList.add(new Sensor(this, "Right", 4));
        sensorList.add(new Sensor(this, "Left", 1));
        sensorList.add(new Sensor(this, "Left", 2));
        sensorList.add(new Sensor(this, "Left", 3));
        sensorList.add(new Sensor(this, "Left", 4));
    }

    /**
     * {@inheritDoc}
     */
    public ConsumingAttribute getDefaultConsumingAttribute() {
        return defaultEffector;
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultConsumingAttribute(ConsumingAttribute consumingAttribute) {
        this.defaultEffector = consumingAttribute;
    }

    /**
     * {@inheritDoc}
     */
    public String getConsumerDescription() {
        return this.getName() + ":" + defaultEffector.getName();
    }

    /**
     * {@inheritDoc}
     */
    public List<ConsumingAttribute> getConsumingAttributes() {
       return effectorList;
    }

    /**
     * {@inheritDoc}
     */
    public ProducingAttribute getDefaultProducingAttribute() {
        return defaultSensor;
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultProducingAttribute(ProducingAttribute producingAttribute) {
        this.defaultSensor = producingAttribute;
    }

    /**
     * {@inheritDoc}
     */
    public String getProducerDescription() {
        return this.getName() + ":" + defaultSensor.getName();
    }

    /**
     * {@inheritDoc}
     */
    public List<ProducingAttribute> getProducingAttributes() {
       return sensorList;
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
        return (orientation * Math.PI) / DEGREES_IN_A_CIRCLE / 2;
    }

    /**
     * Set the orienation of the creature.
     *
     * @param d the orientation, in degrees
     */
    public void setOrientation(final double d) {
        orientation = d;

        if ((d <= 352.5) && (d < 7.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_0.gif"));
        } else if ((d >= 7.5) && (d < 22.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_15.gif"));
        } else if ((d >= 22.5) && (d < 37.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_30.gif"));
        } else if ((d >= 37.5) && (d < 52.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_45.gif"));
        } else if ((d >= 52.5) && (d < 67.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_60.gif"));
        } else if ((d >= 67.5) && (d < 82.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_75.gif"));
        } else if ((d >= 82.5) && (d < 97.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_90.gif"));
        } else if ((d >= 97.5) && (d < 112.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_105.gif"));
        } else if ((d >= 112.5) && (d < 127.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_120.gif"));
        } else if ((d >= 127.5) && (d < 142.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_135.gif"));
        } else if ((d >= 142.5) && (d < 157.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_150.gif"));
        } else if ((d >= 157.5) && (d < 172.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_165.gif"));
        } else if ((d >= 172.5) && (d < 187.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_180.gif"));
        } else if ((d >= 187.5) && (d < 202.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_195.gif"));
        } else if ((d >= 202.5) && (d < 217.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_210.gif"));
        } else if ((d >= 217.5) && (d < 232.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_225.gif"));
        } else if ((d >= 232.5) && (d < 247.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_240.gif"));
        } else if ((d >= 247.5) && (d < 262.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_255.gif"));
        } else if ((d >= 262.5) && (d < 277.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_270.gif"));
        } else if ((d >= 277.5) && (d < 292.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_285.gif"));
        } else if ((d >= 292.5) && (d < 307.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_300.gif"));
        } else if ((d >= 307.5) && (d < 322.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_315.gif"));
        } else if ((d >= 322.5) && (d < 337.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_330.gif"));
        } else if ((d >= 337.5) && (d < 352.5)) {
            getImage().setImage(ResourceManager.getImage("Mouse_345.gif"));
        }
    }


    /**
     * @return position of left whisker, given orientation of creature
     */
    public Point getLeftWhisker() {
        double theta = getOrientationRad();
        int x = (int) (getLocation().x + (whiskerLength * Math.cos(theta + WHISKER_ANGLE)));
        int y = (int) (getLocation().y - (whiskerLength * Math.sin(theta + WHISKER_ANGLE)));

        return new Point(x, y);
    }

    /**
     * @return position of right whisker, given orientation of creature
     */
    public Point getRightWhisker() {
        double theta = getOrientationRad();
        int x = (int) (getLocation().x + (whiskerLength * Math.cos(theta - WHISKER_ANGLE)));
        int y = (int) (getLocation().y - (whiskerLength * Math.sin(theta - WHISKER_ANGLE)));

        return new Point(x, y);
    }

    /**
     * Turn agent to the right.
     * @param value Amount to turn agent
     */
    public void turnRight(final double value) {
        double temp = computeAngle(getOrientation() - (value * turnIncrement));
        setOrientation(temp);
        //System.out.println("Orientation = " + getOrientation());
    }

    /**
     * Turn agent left.
     * @param value Amount to turn agent
     */
    public void turnLeft(final double value) {
        double temp = computeAngle(getOrientation() + (value * turnIncrement));
        setOrientation(temp);
        //System.out.println("Orientation = " + getOrientation());
    }

    /**
     * Ensures that val lies between 0 and 360.
     * @param value the value to compute
     * @return value's "absolute angle"
     */
    private double computeAngle(final double value) {
        double val = value;
        while (val >= DEGREES_IN_A_CIRCLE) {
            val -= DEGREES_IN_A_CIRCLE;
        }

        while (val < 0) {
            val += DEGREES_IN_A_CIRCLE;
        }

        return val;
    }

    /**
     * Moves agent forward.
     * @param value Amount to move agent
     */
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
            moveTo(p.x, p.y);
            wrapAround();
        }
    }

    /**
     * Moves agent backward.
     * @param value Amount to move agent
     */
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
            moveTo(p.x, p.y);
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
//        if ((this.getParent().getUseLocalBounds()) && !this.getParent().contains(possibleCreatureLocation)) {
//            return false;
//        }
//
//        if (!this.getParent().getObjectInhibitsMovement()) {
//            return true;
//        }

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
//        if (this.getParent().getUseLocalBounds()) {
//            return;
//        }

//        if (getLocation().x >= this.getParent().getWorldWidth()) {
//            getLocation().x -= this.getParent().getWorldWidth();
//        }
//
//        if (getLocation().x < 0) {
//            getLocation().x += this.getParent().getWorldWidth();
//        }
//
//        if (getLocation().y >= this.getParent().getWorldHeight()) {
//            getLocation().y -= this.getParent().getWorldHeight();
//        }
//
//        if (getLocation().y < 0) {
//            getLocation().y += this.getParent().getWorldHeight();
//        }
    }

    /**
     * Initiate a motor command based on a string representation of the command.
     *
     * @param commandList the command itself
     * @param value the activation level of the output neuron which produced this command
     */
    public void setMotorCommand(final String command, final double value) {

        if (command.equals("Forward")) {
            goStraightForward(value);
        } else if (command.equals("Backward")) {
            goStraightBackward(value);
        } else if (command.equals("Left")) {
            turnLeft(value);
        } else if (command.equals("Right")) {
            turnRight(value);
        } else {
            absoluteMovement(command, value);
        }
        //TODO: Fire event (also search for repaints)
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
    // Possibly do an overall update on a "pre-pull" method?

    /**
     * Get the stimulus associated with the a given sensory id.
     * @param sensorName the string representing the id
     * @return the stimulus for the id
     */
    public double getStimulus(final String sensorName, final int stimulusDimension) {
        int max = this.getParent().getHighestDimensionalStimulus();
        double[] currentStimulus = SimbrainMath.zeroVector(max);
        AbstractEntity temp = null;
        double distance = 0;

        // Handle X and Y coordinates
        if (sensorName.equals("X")) {
            return this.getX();
        } else if (sensorName.equals("Y")) {
            return this.getY();
        }

        //Sum proximal stimuli corresponding to each object
        if (sensorName.equals("Center")) {
            for (int i = 0; i < this.getParent().getAbstractEntityList().size(); i++) {
                temp = (AbstractEntity) this.getParent().getAbstractEntityList().get(i);
                distance = SimbrainMath.distance(temp.getLocation(), getLocation());

                if (temp == this) {
                    continue;
                }

                currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
            }
        } else if (sensorName.equals("Left")) {
            for (int i = 0; i < this.getParent().getAbstractEntityList().size(); i++) {
                temp = (AbstractEntity) this.getParent().getAbstractEntityList().get(i);
                distance = SimbrainMath.distance(temp.getLocation(), getLeftWhisker());

                if (temp == this) {
                    continue;
                }

                currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
            }
        } else if (sensorName.equals("Right")) {
            for (int i = 0; i < this.getParent().getAbstractEntityList().size(); i++) {
                temp = (AbstractEntity) this.getParent().getAbstractEntityList().get(i);
                distance = SimbrainMath.distance(temp.getLocation(), getRightWhisker());

                if (temp == this) {
                    continue;
                }

                currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
            }
        }

        return currentStimulus[stimulusDimension];
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
    public double getWHISKER_ANGLE() {
        return WHISKER_ANGLE;
    }

    /**
     * @param whiskerAngle The whiskerAngle to set.
     */
    public void setWhiskerAngle(final double whiskerAngle) {
        this.WHISKER_ANGLE = whiskerAngle;
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

}
