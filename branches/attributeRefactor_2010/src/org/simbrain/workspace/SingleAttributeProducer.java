package org.simbrain.workspace;

import java.util.Collections;
import java.util.List;

/**
 * Implements a producer that only provides a single attribute.
 * 
 * @author Matt Watson
 *
 * @param <E> the type that the attribute holds.
 */
public abstract class SingleAttributeProducer<E> extends AbstractAttribute implements Producer, ProducingAttribute<E> {

    /**
     * Returns this object.
     * 
     * @return This object.
     */
    public ProducingAttribute<E> getAttribute() {
        return this;
    }
    
    /**
     * Returns a list containing this object.
     * 
     * @return A list containing this object.
     */
    public List<ProducingAttribute<E>> getProducingAttributes() {
        return Collections.singletonList(getAttribute());
    }
}
