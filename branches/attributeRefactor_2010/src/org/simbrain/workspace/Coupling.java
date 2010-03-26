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
    private Producer<E> Producer; //TODO: Change to lowercase

    /** Consuming attribute for this coupling. */
    private Consumer<E> Consumer;

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
        this.Consumer = Consumer;
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
        this.Producer = Producer;
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

        this.Producer = Producer;
        this.Consumer = Consumer;
    }


    /**
     * Set value of buffer.
     */
    public void setBuffer() {
        final WorkspaceComponent producerComponent
            = Producer.getParentComponent();

        try {
            buffer = Workspace.syncRest(producerComponent.getLocks().iterator(), new Callable<E>() {
                public E call() throws Exception {
                    return Producer.getValue();
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
        if ((Consumer != null) && (Producer != null)) {
            final WorkspaceComponent consumerComponent
                = Consumer.getParentComponent();
            try {
                Workspace.syncRest(consumerComponent.getLocks().iterator(),
                        new Callable<E>() {
                            public E call() throws Exception {
                                Consumer.setValue(buffer);
                                LOGGER.debug(Consumer.getParentComponent()
                                        .getDescription()
                                        + " just consumed "
                                        + Producer.getValue()
                                        + " from "
                                        + Producer.getParentComponent()
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
        return Producer;
    }

    /**
     * @return the Consumer
     */
    public Consumer<E> getConsumer() {
        return Consumer;
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
        if (Producer == null) {
            producerString = "Null";
        } else {
            producerComponent =  "[" + Producer.getParentComponent().toString() +"]";
            producerString = Producer.getDescription();
        }
        if (Consumer == null) {
            consumerString = "Null";
        } else {
            consumerComponent = "[" + Consumer.getParentComponent().toString() +"]";
            consumerString = Consumer.getDescription();
        }
        return  producerComponent + " " + producerString +  " --> " + consumerComponent + " " + consumerString;
     }
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object o) {
        if (o instanceof Coupling) {
            Coupling<?> other = (Coupling<?>) o;
            
            return other.Producer.equals(Producer)
                && other.Consumer.equals(Consumer);
        } else {
            return false;
        }
    }
    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return Producer.hashCode() + (ARBITRARY_PRIME * Consumer.hashCode());
    }
}
