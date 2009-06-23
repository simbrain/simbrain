package org.simbrain.world.odorworld.sensors;

import java.util.List;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Very simple bump sensor. Holding off on more sophisticated "touch" sensors in
 * case an existing¯ library can provide it.
 * 
 * TODO: - Not tested yet Possible extensions: - location of bump sensor -
 * return vector represent impact on agent
 */
public class BumpSensor implements Sensor {

    /** Whether it was bumped. */
    private boolean wasBumped = false;

    /** Value to produce when bumped. */
    private double bumpValue = 0;

    /** Parent agent. */
    private OdorWorldEntity parent;

    /**
     * Construct bump sensor.
     * 
     * @param parent
     * @param name
     * @param bumpVal
     */
    public BumpSensor(OdorWorldEntity parent, String name, double bumpVal) {
        this.parent = parent;
        this.bumpValue = bumpVal;
    }

    /**
     * {@inheritDoc}
     */
    public void update() {
//        if (wasBumped()) {
//            return new Double(bumpValue);
//        } else
//            return new Double(0);
    }

    /**
     * @return the wasBumped
     */
    public boolean wasBumped() {
        return wasBumped;
    }

    /**
     * @param wasBumped
     *            the wasBumped to set
     */
    public void setBumped(boolean wasBumped) {
        this.wasBumped = wasBumped;
    }

    public List<Class> getApplicableTypes() {
        // TODO Auto-generated method stub
        return null;
    }

}
