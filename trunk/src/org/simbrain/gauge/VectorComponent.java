package org.simbrain.gauge;

import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.ProducingAttribute;
import org.simnet.interfaces.Neuron;

public class VectorComponent implements Consumer {

    int dimension = 0;

    double value = 0;

    ConsumingAttribute defaultConsumingAttribute;

    public VectorComponent(int dimension) {
        this.dimension = dimension;
        defaultConsumingAttribute = new ValueConsumer();
    }
    public String getConsumerDescription() {
        return "Component " + dimension;
    }

    public List<ConsumingAttribute> getConsumingAttributes() {
        return java.util.Collections.singletonList(defaultConsumingAttribute);
    }

    public ConsumingAttribute getDefaultConsumingAttribute() {
        return defaultConsumingAttribute;
    }

    public void setDefaultConsumingAttribute(ConsumingAttribute consumingAttribute) {        
    }
    
    private class ValueConsumer implements ConsumingAttribute<Double>{
        public String getName() {
            return "Value";
        }
        public void setValue(Double value) {
            getParent().setValue(value);
        }
        public VectorComponent getParent() {
            return VectorComponent.this;
        }

    }   

    /**
     * @return the value
     */
    public double getValue() {
        return value;
    }
    /**
     * @param value the value to set
     */
    public void setValue(double value) {
        this.value = value;
    }
}