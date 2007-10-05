package org.simbrain.workspace;

import org.apache.log4j.Logger;


/**
 * Coupling between a producing attribute and a consuming attribute.
 *
 * @param <E> coupling attribute value type
 */
public final class Coupling<E> {
    private static final Logger LOGGER = Logger.getLogger(Coupling.class);
    
    /** Producing attribute for this coupling. */
    private ProducingAttribute<E> producingAttribute;

    /** Consuming attribute for this coupling. */
    private final ConsumingAttribute<E> consumingAttribute;

    /** Value of buffer. */
    private E buffer;

    /**
     * Create a coupling between a specified consuming attribute, without yet specifying
     * the corresponding producing attribute.
     *
     * @param consumingAttribute the attribute that consumes.
     */
    public Coupling(final ConsumingAttribute<E> consumingAttribute) {
        super();
        this.consumingAttribute = consumingAttribute;
    }

    /**
     * Create a new coupling between the specified producing attribute
     * and consuming attribute.
     *
     * @param producingAttribute producing attribute for this coupling
     * @param consumingAttribute consuming attribute for this coupling
     */
    public Coupling(final ProducingAttribute<E> producingAttribute,
                    final ConsumingAttribute<E> consumingAttribute) {
        LOGGER.debug("creating a new Coupling");
        LOGGER.trace("producingAttribute: " + producingAttribute);
        LOGGER.trace("consumingAttribute: " + consumingAttribute);
        
        this.producingAttribute = producingAttribute;
        this.consumingAttribute = consumingAttribute;
    }

    /**
     * Set value of buffer.
     */
    public void setBuffer() {
        buffer = producingAttribute.getValue();
    }

    /**
     * Update this coupling.
     */
    public void update() {
        LOGGER.debug("updating coupling");
        if ((consumingAttribute != null) && (producingAttribute != null)) {
            consumingAttribute.setValue(buffer);
            //System.out.println(consumingAttribute.getParent().getConsumerDescription() + " just consumed " + producingAttribute.getValue() + " from " + producingAttribute.getParent().getProducerDescription());
        }
    }


    /**
     * @return the producingAttribute
     */
    public ProducingAttribute<E> getProducingAttribute() {
        return producingAttribute;
    }


    /**
     * @param producingAttribute the producingAttribute to set
     */
    public void setProducingAttribute(ProducingAttribute<E> producingAttribute) {
        this.producingAttribute = producingAttribute;
    }


    /**
     * @return the consumingAttribute
     */
    public ConsumingAttribute<E> getConsumingAttribute() {
        return consumingAttribute;
    }

    /**
     * Used by GUI.
     */
    public String toString() {
        String producerString;
        if (producingAttribute   == null) {
            producerString = "Unbound";
        } else {
            producerString = producingAttribute.getParent().getProducerDescription();
        }
        return consumingAttribute.getParent().getConsumerDescription() + "[" + producerString + "]";
    }
}
