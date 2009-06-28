package org.simbrain.world.odorworld.attributes;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.odorworld.sensors.SmellSensor;

/**
 * Represents a smell sensor as a workspace producer, providing attributes for couplings.
 *
 * @author jyoshimi
 */
public class SmellProducer implements Producer {
    
    /** Parent component for this attribute holder. */
    WorkspaceComponent parentComponent;

    /** The underlying smell sensor. */
    private final SmellSensor sensor;
    
    /** The producing attributes. */
    private ArrayList<ProducingAttribute<?>> producingAttributes
        = new ArrayList<ProducingAttribute<?>>();

    /**
     * Construct a smell producer using a specified smell sensor.
     *
     * @param component parent component
     * @param sensor smell sensor to use
     */
    public SmellProducer(final WorkspaceComponent component, final SmellSensor sensor) {
        this.parentComponent = component;
        this.sensor = sensor;
        
        for (int i = 0; i < sensor.getCurrentValue().length; i++) {
            producingAttributes.add(new SmellAttribute(i));
        }
    }

    /**
     * Smell attribute.
     */
    class SmellAttribute extends AbstractAttribute implements ProducingAttribute<Double> {

        /** Index of the component of the smell vector to sample. */
        int index;
        
        /**
         * Construct a smell attribute.
         *
         * @param i index of smell vector to sample.
         */
        public SmellAttribute(final int i) {
            index = i;
        }
        
        /**
         * {@inheritDoc}
         */
        public Producer getParent() {
            return SmellProducer.this;
        }

        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return Double.valueOf(sensor.getCurrentValue()[index]);
        }

        /**
         * {@inheritDoc}
         */
        public String getKey() {
            return "Smell-" + (index + 1);
        }
        
    }
        
    /**
     * {@inheritDoc}
     */
    public List<? extends ProducingAttribute<?>> getProducingAttributes() {
        return producingAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "Smell sensor";
    }

    /**
     * {@inheritDoc}
     */
    public WorkspaceComponent getParentComponent() {
        return parentComponent;
    }
    
}
