package org.simbrain.workspace;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.simbrain.workspace.updator.WorkspaceUpdator;

/**
 * Coupling between a producing attribute and a consuming attribute.
 *
 * @param <E> coupling attribute value type
 */
public final class Coupling<E> {
    
    /** The static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Coupling.class);
    
    /** An arbitrary prime for creating better hash distributions. */
    private static final int ARBITRARY_PRIME = 59;
    
    /** Producing attribute for this coupling. */
    private ProducingAttribute<E> producingAttribute;

    /** Consuming attribute for this coupling. */
    private ConsumingAttribute<E> consumingAttribute;

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
     * Create a coupling between a specified producing attribute, without yet specifying
     * the corresponding consuming attribute.
     *
     * @param producingAttribute the attribute that produces.
     */
    public Coupling(final ProducingAttribute<E> producingAttribute) {
        super();
        this.producingAttribute = producingAttribute;
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
        LOGGER.debug("new Coupling");
        LOGGER.debug("producing " + producingAttribute.getAttributeDescription());
        LOGGER.debug("consuming " + consumingAttribute.getAttributeDescription());
        
        this.producingAttribute = producingAttribute;
        this.consumingAttribute = consumingAttribute;
    }


    /**
     * Set value of buffer.
     */
    public void setBuffer() {
        final WorkspaceComponent<?> producerComponent
            = producingAttribute.getParent().getParentComponent();
        
        try {
            buffer = Workspace.syncRest(producerComponent.getLocks().iterator(), new Callable<E>() {
                public E call() throws Exception {
                    return producingAttribute.getValue();
                }
            });
        } catch (Exception e) {
            // TODO exception service?
            e.printStackTrace();
        }
        
        LOGGER.debug("buffer set: " + buffer);
    }

    /**
     * Update this coupling.
     */
    public void update() {
        if ((consumingAttribute != null) && (producingAttribute != null)) {
            final WorkspaceComponent<?> consumerComponent
                = consumingAttribute.getParent().getParentComponent();
            
            try {
                Workspace.syncRest(consumerComponent.getLocks().iterator(), new Callable<E>() {
                    public E call() throws Exception {
                        consumingAttribute.setValue(buffer);
                        LOGGER.debug(consumingAttribute.getParent().getDescription()
                            + " just consumed " + producingAttribute.getValue() + " from "
                            + producingAttribute.getParent().getDescription());
                        
                        return null;
                    }
                });
            } catch (Exception e) {
                // TODO exception service?
                e.printStackTrace();
            }
        }
    }


    /**
     * @return the producingAttribute
     */
    public ProducingAttribute<E> getProducingAttribute() {
        return producingAttribute;
    }

    /**
     * @return the consumingAttribute
     */
    public ConsumingAttribute<E> getConsumingAttribute() {
        return consumingAttribute;
    }

    /**
     * Returns the string representation of this coupling.
     * 
     * @return The string representation of this coupling.
     */
    public String toString() {
        String producerString;
        String producerComponent = "";
        String consumerString;
        String consumerComponent = "";
        if (producingAttribute == null) {
            producerString = "Null";
        } else {
            producerComponent =  "[" + producingAttribute.getParent().getParentComponent().toString() +"]";
            producerString = producingAttribute.getAttributeDescription();
        }
        if (consumingAttribute == null) {
            consumerString = "Null";
        } else {
            consumerComponent = "[" + consumingAttribute.getParent().getParentComponent().toString() +"]";
            consumerString = consumingAttribute.getAttributeDescription();
        }
        return  producerComponent + " " + producerString +  " --> " + consumerComponent + " " + consumerString;
     }
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object o) {
        if (o instanceof Coupling) {
            Coupling<?> other = (Coupling<?>) o;
            
            return other.producingAttribute.equals(producingAttribute)
                && other.consumingAttribute.equals(consumingAttribute);
        } else {
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return producingAttribute.hashCode() + (ARBITRARY_PRIME * consumingAttribute.hashCode());
    }
}
