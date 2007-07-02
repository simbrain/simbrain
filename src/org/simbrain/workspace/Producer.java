package org.simbrain.workspace;

import java.util.List;

/**
 * Producer.
 */
public interface Producer {

    /**
     * Return an umodifiable list of producing attributes for this producer.
     * The list may be empty but may not be null.
     *
     * @return an unmodifiable list of producing attributes for this producer
     */
    public List<ProducingAttribute> getProducingAttributes();

    /**
     * Returns the default attribute for this producer.
     *
     * @return the default attribute
     */
    public ProducingAttribute getDefaultProducingAttribute();

    /**
     * Sets the default producing attribute for this producer.
     *
     * @param producingAttribute the default producing attribute to set.
     */
    public void setDefaultProducingAttribute(ProducingAttribute producingAttribute);

    /**
     * Returns a String which describes this producer.
     *
     * @return a string description.
     */
    public String getProducerDescription();


}
