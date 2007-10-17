package org.simbrain.workspace;

import java.util.Collections;
import java.util.List;

public abstract class SingleAttributeProducer<E> implements Producer, ProducingAttribute<E> {

    public ProducingAttribute<E> getDefaultProducingAttribute() {
        return this;
    }
    
    public List<ProducingAttribute<E>> getProducingAttributes() {
        return Collections.singletonList(getDefaultProducingAttribute());
    }
}
