package org.simbrain.workspace;

import java.util.Collections;
import java.util.List;

/**
 * Implements a consumer that only provides a single attribute.
 * 
 * @author Matt Watson
 *
 * @param <E> the type that the attribute holds.
 */
public abstract class SingleAttributeConsumer<E> extends AbstractAttribute implements Consumer, ConsumingAttribute<E> {

    /**
     * Returns this object.
     * 
     * @return This object.
     */
    public ConsumingAttribute<E> getAttribute() {
        return this;
    }

    /**
     * Returns a list containing this object.
     * 
     * @return a list containing this object.
     */
    public List<ConsumingAttribute<E>> getConsumingAttributes() {
        return Collections.singletonList(getAttribute());
    }

    /**
     * Returns this object.
     * 
     * @return This object.
     */
    public final Consumer getParent() {
        return this;
    }
    
}
