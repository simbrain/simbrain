package org.simbrain.workspace;

import java.lang.reflect.Type;
import java.util.Arrays;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;

public class Coupling2<T> {

    final Producer2<T> producer;
    final Consumer2<T> consumer;

    public Coupling2(Producer2<T> producer, Consumer2<T> consumer)
            throws MismatchedAttributesException {
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
            producerString = "Null";
        } else {
            // producerComponent = "[" + producer.getParentComponent().getName()
            // + "]";
            // producerString = producer.getDescription();
            producerString = producer.toString();
        }
        if (consumer == null) {
            consumerString = "Null";
        } else {
            // consumerComponent = "[" + consumer.getParentComponent().getName()
            // + "]";
            // consumerString = consumer.getDescription();
            consumerString = consumer.toString();
        }
        return producerComponent + " " + producerString + " --> "
                + consumerComponent + " " + consumerString;
    }

    public static void main(String[] args) {
        NetworkComponent nc = new NetworkComponent("");
        System.out.println("Testing");
        NeuronGroup group1 = new NeuronGroup(nc.getNetwork(), 10);
        NeuronGroup group2 = new NeuronGroup(nc.getNetwork(), 10);

        for (int i = 0; i < group1.size(); i++) {
            group1.getNeuronList().get(i).forceSetActivation(i);
        }
        nc.getNetwork().addGroup(group1);
        nc.getNetwork().addGroup(group2);

        Producer2 group1Producer = nc.getProducers(group1)
                .get(0);
        Consumer2 group2Consumer = nc.getConsumers(group2)
                .get(0);

        System.out.println("Before" + Arrays.toString(group1.getActivations()));
        System.out.println("Before" + Arrays.toString(group2.getActivations()));
        Coupling2<double[]> coupling;
        try {
            coupling = new Coupling2<>(group1Producer, group2Consumer);
            coupling.update();
        } catch (MismatchedAttributesException e) {
            e.printStackTrace();
        }
        System.out.println("After" + Arrays.toString(group1.getActivations()));
        System.out.println("After" + Arrays.toString(group2.getActivations()));

    }

    //TODO.  Currently used for coupling action de-serializtion, which does not yet
    // work.  If not used, get rid of this.
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
