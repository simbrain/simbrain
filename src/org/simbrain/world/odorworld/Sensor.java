package org.simbrain.world.odorworld;

import org.simbrain.workspace.ProducingAttribute;


/**
 * <b>Sensors</b> represent sensors which produce inputs to other components.
 */
public class Sensor implements ProducingAttribute<Double> {

    /** Which dimension of the stimulus to read. */
    private int stimulusDimension = 0;

    /** Left, Right, Center, for now. */
    private String name;

    /** Reference to parent. */
    private OdorWorldAgent parent;

    /**
     * Construct a sensor.
     *
     * @param parent reference
     * @param sensorName name
     * @param dim stimulus dimension
     */
    public Sensor(final OdorWorldAgent parent, final String name, final int dim) {
        this.parent = parent;
        this.name = name;
        stimulusDimension = dim;
    }

    /**
     * {@inheritDoc}
     */
    public String getAttributeDescription() {
        return name + "[" + stimulusDimension + "]";
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue() {
        return getParent().getStimulus(name, stimulusDimension);
    }

    /**
     * {@inheritDoc}
     */
    public OdorWorldAgent getParent() {
        return parent;
    }

}
