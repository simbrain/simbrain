package org.simbrain.workspace;

import java.lang.reflect.Type;

/**
 * A pair containing a producer and a consumer.  When updated, the producer produces a value
 * and the consumer consumes it.  See http://www.simbrain.net/Documentation/docs/Pages/Workspace/Couplings.html
 * and https://www.youtube.com/watch?v=zDUY9mUKZ-I
 *
 * @param <T> the type of the coupling. E.g a double coupling is a link that gets a double value from a producer
 *           and then sets a double value on a consumer.
 *
 * @author Jeff Yoshimi
 * @author Tim Shea
 * @author Matt Watson
 */
public class Coupling<T> {

    /**
     * The producer in the coupling.
     */
    private final Producer<T> producer;

    /**
     * The consumer in the coupling.
     */
    private final Consumer<T> consumer;

    /**
     * Private constructor.  Static creation method is used.
     */
    private Coupling(Producer<T> producer, Consumer<T> consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }

    /**
     * This is the main action!  Set the value of the consumer based on the
     * value of the producer.
     * <br>
     * Note that values are passed by reference, so that it is up to the producing or
     * consuming methods to make defensive copies as needed.
     * (cf http://www.javapractices.com/topic/TopicAction.do?Id=15)).
     */
    public void update() {
        consumer.setValue(producer.getValue());
    }

    /**
     * Returns a default formatted description of a coupling.
     *
     * @return the description
     */
    public String getDescription() {
        String producerString = producer == null ? "None" : producer.toString();
        String consumerString = consumer == null ? "None" : consumer.toString();
        return producerString + " > " + consumerString;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    public String getId() {
        return producer.getId() + " > " + consumer.getId();
    }

    public Type getType() {
        return producer.getType();
    }

    public Producer<T> getProducer() {
        return producer;
    }

    public Consumer<T> getConsumer() {
        return consumer;
    }

    /**
     * Main creation  method for couplings.
     *
     * @param producer the producer
     * @param consumer the consumer
     * @param <S> the type of the coupling (usually double or double[])
     * @return the coupling
     * @throws MismatchedAttributesException
     */
    static <S> Coupling<S> create(Producer<S> producer, Consumer<S> consumer) throws MismatchedAttributesException {
        if (producer.getType() == consumer.getType()) {
            return new Coupling<S>(producer, consumer);
        } else {
            throw new MismatchedAttributesException(String.format("Producer type %s does not match consumer type %s", producer.getType(), consumer.getType()));
        }
    }
}
