package org.simbrain.workspace;


/**
 * TODO.
 */
public interface Producer<E> extends Attribute {

    /**
     * Return the value for this producer.
     *
     * @return the value for this producer
     */
    E getValue();

}
