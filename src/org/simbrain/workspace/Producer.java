package org.simbrain.workspace;

import java.util.List;

/**
 * Producer.
 */
public interface Producer extends AttributeHolder {

    /**
     * Return an unmodifiable list of producing attributes for this producer.
     * The list may be empty but may not be null.
     *
     * @return an unmodifiable list of producing attributes for this producer
     */
    public abstract List<? extends ProducingAttribute<?>> getProducingAttributes();

    /**
     * Returns the default attribute for this producer.
     *
     * @return the default attribute
     */
    public abstract ProducingAttribute<?> getDefaultProducingAttribute();

    /**
     * Sets the default producing attribute for this producer.
     *
     * @param producingAttribute the default producing attribute to set.
     */
     // TODO is this needed?
//    public void setDefaultProducingAttribute(ProducingAttribute<?> producingAttribute);
}
