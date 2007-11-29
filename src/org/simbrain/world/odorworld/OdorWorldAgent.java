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

import java.awt.Image;
import java.awt.Point;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

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
    private ArrayList<ConsumingAttribute<?>> effectorList = new ArrayList<ConsumingAttribute<?>>();

    /** Default effector. */
    private ConsumingAttribute<Double> defaultEffector = new Forward();

    /** List of sensors. */
    private ArrayList<ProducingAttribute<?>> sensorList = new ArrayList<ProducingAttribute<?>>();

    /** Default sensor. */
    private ProducingAttribute<Double> defaultSensor = new CenterSensor(1);

    private final OdorWorldComponent component;

    /**
     * Default constructor.
     */
    private OdorWorldAgent(OdorWorldComponent component) {
        this.component = component;
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
    public OdorWorldAgent(final OdorWorldComponent component, final String nm,
            final String type, final int x, final int y, final double ori) {
        super(component.getWorld(), type, x, y);
        super.setName(nm);
        this.component = component;
        setOrientation(ori);
        initEffectorsAndSensors();

    }
    
    /**
     * Initialize effectors and sensors, thereby allowing other Simbrain components to couple to the agent.
     */
    public void initEffectorsAndSensors() {
        effectorList.add(defaultEffector);
        effectorList.add(new Right());
        effectorList.add(new Left());
        effectorList.add(new North());
        effectorList.add(new South());
        effectorList.add(new East());
        effectorList.add(new West());
        
        sensorList.add(defaultSensor);
        sensorList.add(new CenterSensor(2));
        sensorList.add(new CenterSensor(3));
        sensorList.add(new CenterSensor(4));
        sensorList.add(new RightWhisker(1));
        sensorList.add(new RightWhisker(2));
        sensorList.add(new RightWhisker(3));
        sensorList.add(new RightWhisker(4));
        sensorList.add(new LeftWhisker(1));
        sensorList.add(new LeftWhisker(2));
        sensorList.add(new LeftWhisker(3));
        sensorList.add(new LeftWhisker(4));
    }

    /**
     * {@inheritDoc}
     */
    public ConsumingAttribute<Double> getDefaultConsumingAttribute() {
        return defaultEffector;
    }

    /**
     * {@inheritDoc}
     */
   public void setDefaultConsumingAttribute(final ConsumingAttribute consumingAttribute) {
      defaultEffector = consumingAttribute;
   }

    /**
     * {@inheritDoc}
     */
    public void setDefaultProducingAttribute(final ProducingAttribute producingAttribute) {
       defaultSensor = producingAttribute;
    }


    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return this.getName();
    }

    /**
     * {@inheritDoc}
     */
    public ArrayList<ConsumingAttribute<?>> getConsumingAttributes() {
       return effectorList;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayList<ProducingAttribute<?>> getProducingAttributes() {
       return sensorList;
    }

    /**
     * {@inheritDoc}
     */
    public ProducingAttribute<Double> getDefaultProducingAttribute() {
        return defaultSensor;
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
        return (orientation * Math.PI) / (DEGREES_IN_A_CIRCLE / 2);
    }

    static final TreeMap<Double, Image> images = new TreeMap<Double, Image>();
    
    /* initialize images */
    static {
        images.put(7.5, ResourceManager.getImage("Mouse_0.gif"));
        images.put(22.5, ResourceManager.getImage("Mouse_15.gif"));
        images.put(37.5, ResourceManager.getImage("Mouse_30.gif"));
        images.put(52.5, ResourceManager.getImage("Mouse_45.gif"));
        images.put(67.5, ResourceManager.getImage("Mouse_60.gif"));
        images.put(82.5, ResourceManager.getImage("Mouse_75.gif"));
        images.put(97.5, ResourceManager.getImage("Mouse_90.gif"));
        images.put(112.5, ResourceManager.getImage("Mouse_105.gif"));
        images.put(127.5, ResourceManager.getImage("Mouse_120.gif"));
        images.put(142.5, ResourceManager.getImage("Mouse_135.gif"));
        images.put(157.5, ResourceManager.getImage("Mouse_150.gif"));
        images.put(172.5, ResourceManager.getImage("Mouse_165.gif"));
        images.put(187.5, ResourceManager.getImage("Mouse_180.gif"));
        images.put(202.5, ResourceManager.getImage("Mouse_195.gif"));
        images.put(217.5, ResourceManager.getImage("Mouse_210.gif"));
        images.put(232.5, ResourceManager.getImage("Mouse_225.gif"));
        images.put(247.5, ResourceManager.getImage("Mouse_240.gif"));
        images.put(262.5, ResourceManager.getImage("Mouse_255.gif"));
        images.put(277.5, ResourceManager.getImage("Mouse_270.gif"));
        images.put(292.5, ResourceManager.getImage("Mouse_285.gif"));
        images.put(307.5, ResourceManager.getImage("Mouse_300.gif"));
        images.put(322.5, ResourceManager.getImage("Mouse_315.gif"));
        images.put(337.5, ResourceManager.getImage("Mouse_330.gif"));
        images.put(352.5, ResourceManager.getImage("Mouse_345.gif"));
    }
    
    /**
     * Set the orienation of the creature.
     *
     * @param d the orientation, in degrees
     */
    public void setOrientation(final double d) {
        orientation = d;

        SortedMap<Double, Image> headMap = images.headMap(orientation);
        
        Image image = headMap.size() > 0 ? 
          images.get(headMap.lastKey()) : images.get(images.firstKey());
        
        setImage(image);
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
        if (this.getParent().isUseLocalBounds()) {
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
    
    @Override
    public void setLocation(Point p)
    {
        super.setLocation(p);
        wrapAround();
    }

    //TODO: Do this operation for just the stim_id requested.  Make more efficient: Talk to Scott.
    // Possibly do an overall update on a "pre-pull" method?

    /**
     * Get the stimulus associated with the a given sensory id.
     * @param sensorName the string representing the id
     * @return the stimulus for the id
     */
    public double getStimulus(Sensor sensor) {
        int max = this.getParent().getHighestDimensionalStimulus();
        double[] currentStimulus = SimbrainMath.zeroVector(max);
        
        // Handle X and Y coordinates
        // TODO is this needed for anything?
//        if (sensor.getName().equals("X")) {
//            return this.getX();
//        } else if (sensor.getName().equals("Y")) {
//            return this.getY();
//        }

        //Sum proximal stimuli corresponding to each object
        for (int i = 0; i < this.getParent().getAbstractEntityList().size(); i++) {
            AbstractEntity temp = (AbstractEntity) this.getParent().getAbstractEntityList().get(i);
            double distance = sensor.getDistance(temp.getLocation());

            if (temp == this) {
                continue;
            }

            currentStimulus = SimbrainMath.addVector(currentStimulus, temp.getStimulus().getStimulus(distance));
        }

        return currentStimulus[sensor.getStimulusDimension()];
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
    // TODO why is whisker angle static?
    public void setWhiskerAngle(final double whiskerAngle) {
        OdorWorldAgent.WHISKER_ANGLE = whiskerAngle;
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

    public OdorWorldComponent getParentComponent() {
        return component;
    }
    
    public OdorWorldAgent copy() {
        OdorWorldAgent temp = new OdorWorldAgent(component);
        temp.setImageName(getImageName());
        temp.setMovementIncrement(getMovementIncrement());
        temp.setName("Copy of " + getName());
        temp.setOrientation(getOrientation());
        temp.setStimulus(getStimulus());
        temp.setImage(getImage().getImage());
        temp.setTurnIncrement(getTurnIncrement());
        temp.setWhiskerAngle(getWHISKER_ANGLE());
        temp.setWhiskerLength(getWhiskerLength());
        
        return temp;
    }
    
    abstract class Sensor implements ProducingAttribute<Double> {
        /** Which dimension of the stimulus to read. */
        private final int stimulusDimension;

        private String name;
        
        /**
         * Construct a sensor.
         *
         * @param parent reference
         * @param sensorName name
         * @param dim stimulus dimension
         */
        public Sensor(String name, final int dim) {
            this.name = name;
            this.stimulusDimension = dim;
        }

        public int getStimulusDimension() {
            return stimulusDimension;
        }
        
        /**
         * {@inheritDoc}
         */
        public String getAttributeDescription() {
            return getName() + "[" + stimulusDimension + "]";
        }

        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return getStimulus(this);
        }

        /**
         * {@inheritDoc}
         */
        public OdorWorldAgent getParent() {
            return OdorWorldAgent.this;
        }

        public Type getType() {
            return Double.TYPE;
        }
        
        String getName() {
            return name;
        }
        
        abstract Point location();
        
        public double getDistance(Point from) {
            return SimbrainMath.distance(from, location());
        }
    }
    
    class CenterSensor extends Sensor {
        public CenterSensor(int dim) {
            super("Center", dim);
        }

        @Override
        public Point location() {
            return getLocation();
        }
    }
    
    class RightWhisker extends Sensor {
        public RightWhisker(int dim) {
            super("Right", dim);
        }

        @Override
        public Point location() {
            double theta = getOrientationRad();
            int x = (int) (getLocation().x + (whiskerLength * Math.cos(theta - WHISKER_ANGLE)));
            int y = (int) (getLocation().y - (whiskerLength * Math.sin(theta - WHISKER_ANGLE)));

            return new Point(x, y);
        }
    };
    
    class LeftWhisker extends Sensor {
        public LeftWhisker(int dim) {
            super("Left", dim);
        }

        @Override
        Point location() {
            double theta = getOrientationRad();
            int x = (int) (getLocation().x + (whiskerLength * Math.cos(theta + WHISKER_ANGLE)));
            int y = (int) (getLocation().y - (whiskerLength * Math.sin(theta + WHISKER_ANGLE)));

            return new Point(x, y);
        }
    };
    
    /**
     * <b>Effectors</b> represent commands which move an agent around.
     */
    abstract class Effector implements ConsumingAttribute<Double> {

        /** The motor command. Right, Left, etc. */
        private String name;

        /**
         * Construct an effector.
         * @param parent reference to parent.
         * @param command string command.
         */
        public Effector(String name) {
            this.name = name;
        }

        /**
         * {@inheritDoc}
         */
        public String getAttributeDescription() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        public Consumer getParent() {
            return OdorWorldAgent.this;
        }

        /**
         * {@inheritDoc}
         */
        public abstract void setValue(final Double value);

        public Type getType() {
            return Double.TYPE;
        }
    }
    
    class Forward extends Effector {

        public Forward() {
            super("Forward");
        }

        @Override
        public void setValue(Double value) {
            System.out.println("value: " + value);
            goStraightForward(value);
        }
    }
    
    class Backward extends Effector {

        public Backward() {
            super("Backward");
        }

        @Override
        public void setValue(Double value) {
            goStraightForward(value);
        }
    }
    
    class Left extends Effector {

        public Left() {
            super("Left");
        }

        @Override
        public void setValue(Double value) {
            turnLeft(value);
        }
    }
    
    class Right extends Effector {

        public Right() {
            super("Right");
        }

        @Override
        public void setValue(Double value) {
            turnRight(value);
        }
    }
    
    class East extends Effector {

        public East() {
            super("East");
        }

        @Override
        public void setValue(Double value) {
            Point possible = (Point) getLocation().clone();
            int increment = (int) (movementIncrement * value);

            possible.x += increment;

            if (validMove(possible)) {
                setLocation(possible);
            }
        }
    }
    
    class West extends Effector {

        public West() {
            super("West");
        }

        @Override
        public void setValue(Double value) {
            Point possible = (Point) getLocation().clone();
            int increment = (int) (movementIncrement * value);

            possible.x -= increment;

            if (validMove(possible)) {
                setLocation(possible);
            }
        }
    }
    
    class North extends Effector {

        public North() {
            super("North");
        }

        @Override
        public void setValue(Double value) {
            Point possible = (Point) getLocation().clone();
            int increment = (int) (movementIncrement * value);

            possible.y -= increment;

            if (validMove(possible)) {
                setLocation(possible);
            }
        }
    }
    
    class South extends Effector {

        public South() {
            super("South");
        }

        @Override
        public void setValue(Double value) {
            Point possible = (Point) getLocation().clone();
            int increment = (int) (movementIncrement * value);

            possible.y += increment;

            if (validMove(possible)) {
                setLocation(possible);
            }
        }
    }

}
