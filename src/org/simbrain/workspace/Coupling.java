package org.simbrain.workspace;

import java.lang.reflect.Type;

public class Coupling<T> {
    static <S> Coupling<S> create(Producer<S> producer, Consumer<S> consumer)
            throws MismatchedAttributesException {
        if (producer.getType() == consumer.getType()) {
            return new Coupling<S>(producer, consumer);
        } else {
            throw new MismatchedAttributesException(String.format("Producer type %s does not match consumer type %s",
                    producer.getType(), consumer.getType()));
        }
    }

    private Producer<T> producer;
    private Consumer<T> consumer;

    private Coupling(Producer<T> producer, Consumer<T> consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }

    public Type getType() {
        return producer.getType();
    }

    public void update() {
        consumer.setValue(producer.getValue());
    }

    @Override
    public String toString() {
        String producerString = producer == null ? "None" : producer.toString();
        String consumerString = consumer == null ? "None" : consumer.toString();
        return producerString + " > " + consumerString;
    }

    public String getId() {
        return producer.getId() + " > " + consumer.getId();
    }

    /**
     * @return the producer
     */
    public Producer<T> getProducer() {
        return producer;
    }

    /**
     * @return the consumer
     */
    public Consumer<T> getConsumer() {
        return consumer;
    }

}
