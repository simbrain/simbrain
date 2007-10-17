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
    public abstract List<? extends ConsumingAttribute<?>> getConsumingAttributes();

    /**
     * Returns the default attribute for this consumer.
     *
     * @return the default attribute
     */
    public abstract ConsumingAttribute<?> getDefaultConsumingAttribute();

    /**
     * Sets the default consuming attribute for this consumer.
     *
     * @param consumingAttribute the default consuming attribute to set.
     */
    // TODO is this needed?
//    public void setDefaultConsumingAttribute(ConsumingAttribute<?> consumingAttribute);

    /**
     * Returns a String which describes this consumer.
     *
     * @return a string description.
     */
    public abstract String getConsumerDescription();
}
