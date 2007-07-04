package org.simbrain.world.odorworld;

import org.simbrain.workspace.ProducingAttribute;


/**
 * <b>Sensors</b> represent sensors which produce inputs to other components.
 */
public class Sensor implements ProducingAttribute<Double> {

    /** Which dimension of the stimulus to read. */
    private int stimulusDimension = 0;

    /** Left, Right, Center, for now. */
    private String sensorName;

    /** Reference to parent. */
    private OdorWorldAgent parent;

    /**
     * Construct a sensor.
     *
     * @param parent reference
     * @param sensorName name
     * @param dim stimulus dimension
     */
    public Sensor(final OdorWorldAgent parent, final String sensorName, final int dim) {
        this.parent = parent;
        this.sensorName = sensorName;
        stimulusDimension = dim;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return sensorName + ":" + stimulusDimension;
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue() {
        return getParent().getStimulus(sensorName, stimulusDimension);
    }

    /**
     * {@inheritDoc}
     */
    public OdorWorldAgent getParent() {
        return parent;
    }

}
