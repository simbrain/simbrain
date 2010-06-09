package org.simbrain.workspace;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

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
    private Producer<E> producer;

    /** Consuming attribute for this coupling. */
    private Consumer<E> consumer;

    /** Value of buffer. */
    private E buffer;

    /**
     * Create a coupling between a specified consuming attribute, without yet
     * specifying the corresponding producing attribute.
     *
     * @param Consumer
     *            the attribute that consumes.
     */
    public Coupling(final Consumer<E> Consumer) {
        super();
        this.consumer = Consumer;
    }

    /**
     * Create a coupling between a specified producing attribute, without yet
     * specifying the corresponding consuming attribute.
     *
     * @param Producer
     *            the attribute that produces.
     */
    public Coupling(final Producer<E> Producer) {
        super();
        this.producer = Producer;
    }


    /**
     * Create a new coupling between the specified producing attribute
     * and consuming attribute.
     *
     * @param Producer producing attribute for this coupling
     * @param Consumer consuming attribute for this coupling
     */
    public Coupling(final Producer<E> Producer,
                    final Consumer<E> Consumer) {
        LOGGER.debug("new Coupling");
//        System.out.println("producing " + Producer.getAttributeDescription());
//        System.out.println("consuming " + Consumer.getAttributeDescription());

        this.producer = Producer;
        this.consumer = Consumer;
    }


    /**
     * Set value of buffer.
     */
    public void setBuffer() {
        final WorkspaceComponent producerComponent
            = producer.getParentComponent();

        try {
            buffer = Workspace.syncRest(producerComponent.getLocks().iterator(), new Callable<E>() {
                public E call() throws Exception {
                    return producer.getValue();
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
        if ((consumer != null) && (producer != null)) {
            final WorkspaceComponent consumerComponent
                = consumer.getParentComponent();
            try {
                Workspace.syncRest(consumerComponent.getLocks().iterator(),
                        new Callable<E>() {
                            public E call() throws Exception {
                                consumer.setValue(buffer);
                                LOGGER.debug(consumer.getParentComponent()
                                        .getDescription()
                                        + " just consumed "
                                        + producer.getValue()
                                        + " from "
                                        + producer.getParentComponent()
                                                .getDescription());

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
     * @return the Producer
     */
    public Producer<E> getProducer() {
        return producer;
    }

    /**
     * @return the Consumer
     */
    public Consumer<E> getConsumer() {
        return consumer;
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
        if (producer == null) {
            producerString = "Null";
        } else {
            producerComponent =  "[" + producer.getParentComponent().toString() +"]";
            producerString = producer.getDescription();
        }
        if (consumer == null) {
            consumerString = "Null";
        } else {
            consumerComponent = "[" + consumer.getParentComponent().toString() +"]";
            consumerString = consumer.getDescription();
        }
        return  producerComponent + " " + producerString +  " --> " + consumerComponent + " " + consumerString;
     }
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object o) {
        if (o instanceof Coupling) {
            Coupling<?> other = (Coupling<?>) o;
            
            return other.producer.equals(producer)
                && other.consumer.equals(consumer);
        } else {
            return false;
        }
    }
    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return producer.hashCode() + (ARBITRARY_PRIME * consumer.hashCode());
    }
}
