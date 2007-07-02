package org.simbrain.workspace;

import java.util.List;

/**
 * Consumer.
 */
public interface Consumer {

    /**
     * Return an unmodifiable list of consuming attributes for this consumer.
     * The list may be empty but may not be null.
     *
     * @return an unmodifiable list of consuming attributes for this consumer
     */
    public List<ConsumingAttribute> getConsumingAttributes();

    /**
     * Returns the default attribute for this consumer.
     *
     * @return the default attribute
     */
    public ConsumingAttribute getDefaultConsumingAttribute();

    /**
     * Sets the default consuming attribute for this consumer.
     *
     * @param consumingAttribute the default consuming attribute to set.
     */
    public void setDefaultConsumingAttribute(ConsumingAttribute consumingAttribute);

    /**
     * Returns a String which describes this consumer.
     *
     * @return a string description.
     */
    public String getConsumerDescription();
}
