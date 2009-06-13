package org.simbrain.workspace;

import java.util.List;

/**
 * A consumer is an object that contains two or more consuming attributes. If it
 * has just one consuming attribute use SingleAttributeConsumer.
 */
public interface Consumer extends AttributeHolder {

    /**
     * Return an unmodifiable list of consuming attributes for this consumer.
     * The list may be empty but may not be null.
     *
     * @return an unmodifiable list of consuming attributes for this consumer
     */
    List<? extends ConsumingAttribute<?>> getConsumingAttributes();

}
