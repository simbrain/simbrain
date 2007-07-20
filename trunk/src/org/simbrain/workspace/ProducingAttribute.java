package org.simbrain.workspace;

/**
 * Producing attribute.
 *
 * @param <E> attribute value type
 */
public interface ProducingAttribute<E> extends Attribute {

    /**
     * Return a reference to the parent of this attribute
     *
     * @return the parent reference
     */
    Producer getParent();

    /**
     * Return the value for this producing attribute.
     *
     * @return the value for this producing attribute
     */
    E getValue();
}
