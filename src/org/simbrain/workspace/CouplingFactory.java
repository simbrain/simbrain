package org.simbrain.workspace;

import java.util.Collection;
import java.util.Iterator;

/**
 * CouplingFactory provides methods for instantiating couplings between components in a workspace.
 * Simulation and GUI code should rely on the factory methods here rather than invoking constructors
 * on Coupling directly so that couplings will be properly managed and serialized.
 */
public class CouplingFactory {
    /** The workspace in which the couplings created by this factory will be managed. */
    private Workspace workspace;

    /** Create a new CouplingFactory in the specified workspace. */
    public CouplingFactory(Workspace workspace) {
        this.workspace = workspace;
    }

    /** Create a coupling from a producer and consumer of the same type. */
    public <T> Coupling2<T> createCoupling(Producer2<T> producer, Consumer2<T> consumer)
            throws MismatchedAttributesException {
        Coupling2<T> coupling = new Coupling2<T>(producer, consumer);
        workspace.addCoupling(coupling);
        return coupling;
    }

    /**
     * Try to create a coupling from a producer and consumer of the same type, but
     * do nothing if the types do not match and return null.
     */
    @SuppressWarnings("unchecked")
    public Coupling2 tryCoupling(Producer2 producer, Consumer2 consumer) {
        if (producer.getType() == consumer.getType()) {
            try {
                return createCoupling(producer, consumer);
            } catch (MismatchedAttributesException ex) {
                // Should never happen
                throw new AssertionError(ex);
            }
        } else {
            return null;
        }
    }

    /**
     * Create a coupling from each producer to every consumer of the same type.
     * Will throw an exception if any of the types do not match.
     * @param producers A collection of producers to couple
     * @param consumers A collection of consumers to couple
     * @exception MismatchedAttributesException An exception indicating that a pair of attributes did not match
     *     types. This will be thrown for the first such pair encountered.
     */
     public void createOneToManyCouplings(Collection<Producer2<?>> producers, Collection<Consumer2<?>> consumers)
            throws MismatchedAttributesException {
         for (Producer2 producer : producers) {
             for (Consumer2 consumer : consumers) {
                 createCoupling(producer, consumer);
             }
         }
     }

    /**
     * Try to create a coupling from each producer to every consumer, but ignore any mismatched types.
     * @param producers A collection of producers to couple
     * @param consumers A collection of consumers to couple
     */
    public void tryOneToManyCouplings(Collection<Producer2<?>> producers, Collection<Consumer2<?>> consumers) {
        for (Producer2 producer : producers) {
            for (Consumer2 consumer : consumers) {
                tryCoupling(producer, consumer);
            }
        }
    }

    /**
     * Create a coupling from each attribute in the smaller collection to a corresponding attribute.
     * Will throw an exception if any of the types do not match.
     * @param producers A collection of producers to couple
     * @param consumers A collection of consumers to couple
     * @exception MismatchedAttributesException An exception indicating that a pair of attributes did not match
     *     types. This will be thrown for the first such pair encountered.
     */
    public void createOneToOneCouplings(Collection<Producer2<?>> producers, Collection<Consumer2<?>> consumers)
            throws MismatchedAttributesException {
        Iterator<Consumer2<?>> consumerIterator = consumers.iterator();
        for (Producer2<?> producer : producers) {
            if (consumerIterator.hasNext()) {
                Consumer2 consumer = consumerIterator.next();
                createCoupling(producer, consumer);
            } else {
                break;
            }
        }
    }

    /**
     * Try to create a coupling from each attribute in the smaller collection, but ignore any mismatched types.
     * @param producers A collection of producers to couple
     * @param consumers A collection of consumers to couple
     */
    public void tryOneToOneCouplings(Collection<Producer2<?>> producers, Collection<Consumer2<?>> consumers) {
        Iterator<Consumer2<?>> consumerIterator = consumers.iterator();
        for (Producer2 producer : producers) {
            if (consumerIterator.hasNext()) {
                Consumer2 consumer = consumerIterator.next();
                tryCoupling(producer, consumer);
            } else {
                break;
            }
        }
    }
}
