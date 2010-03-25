package org.simbrain.workspace;

import java.util.List;

/**
 * A producer is an object that contains two or more producing attributes. If it
 * has just one producing attribute use SingleAttributeProducer.
 */
public interface Producer extends AttributeHolder {

    /**
     * Return an unmodifiable list of producing attributes for this producer.
     * The list may be empty but may not be null.
     *
     * @return an unmodifiable list of producing attributes for this producer
     */
    List<? extends ProducingAttribute<?>> getProducingAttributes();

}
