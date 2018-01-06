package org.simbrain.workspace;

import java.lang.reflect.Type;

public class Coupling2<T> {
    private Producer2<T> producer;
    private Consumer2<T> consumer;

    public Coupling2(Producer2<T> producer, Consumer2<T> consumer) throws MismatchedAttributesException {
        this.producer = producer;
        this.consumer = consumer;
        // Check that the types of the attributes match
        if (producer.getType() != consumer.getType()) {
            String warning = "Producer type ("
                    + producer.getType().getTypeName()
                    + ") does not match consumer type ("
                    + consumer.getType().getTypeName();
            throw new MismatchedAttributesException(warning);
        }
    }

    public Type getType() {
        return producer.getType();
    }

    public void update() {
        consumer.setValue(producer.getValue());
    }

    @Override
    public String toString() {
        String producerString;
        String producerComponent = "";
        String consumerString;
        String consumerComponent = "";
        if (producer == null) {
            producerString = "None";
        } else {
            producerString = producer.toString();
        }
        if (consumer == null) {
            consumerString = "None";
        } else {
            consumerString = consumer.toString();
        }
        return producerComponent + " " + producerString + " --> "
                + consumerComponent + " " + consumerString;
    }

    public String getId() {
        return producer.getId() + ">" + consumer.getId();
    }

    /**
     * @return the producer
     */
    public Producer2<T> getProducer() {
        return producer;
    }

    /**
     * @return the consumer
     */
    public Consumer2<T> getConsumer() {
        return consumer;
    }

}
