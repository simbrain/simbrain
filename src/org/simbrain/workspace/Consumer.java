package org.simbrain.workspace;

import java.util.List;

/**
 * Consumer.
 */
public interface Consumer extends AttributeHolder {

    /**
     * Return an unmodifiable list of consuming attributes for this consumer.
     * The list may be empty but may not be null.
     *
     * @return an unmodifiable list of consuming attributes for this consumer
     */
    List<? extends ConsumingAttribute<?>> getConsumingAttributes();

    /**
     * Returns the default attribute for this consumer.
     *
     * @return the default attribute
     */
    ConsumingAttribute<?> getDefaultConsumingAttribute();

    /**
     * Sets the default consuming attribute for this consumer.
     *
     * @param consumingAttribute the default consuming attribute to set.
     */
    void setDefaultConsumingAttribute(ConsumingAttribute<?> consumingAttribute);
}
