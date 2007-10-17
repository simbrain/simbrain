package org.simbrain.workspace;

import java.util.Collections;
import java.util.List;

public abstract class SingleAttributeConsumer<E> implements Consumer, ConsumingAttribute<E> {

    public ConsumingAttribute<E> getDefaultConsumingAttribute() {
        return this;
    }
    
    public List<ConsumingAttribute<E>> getConsumingAttributes() {
        return Collections.singletonList(getDefaultConsumingAttribute());
    }
}
