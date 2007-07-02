package org.simbrain.workspace;

/**
 * Consuming attribute.
 *
 * @param <E> attribute value type
 */
public interface ConsumingAttribute<E> {

    /**
     * Return a reference to the parent of this attribute
     *
     * @return the parent reference
     */
    Consumer getParent();
    
    /**
     * Return the name of this consuming attribute.
     *
     * @return the name of this consuming attribute
     */
    String getName();

    /**
     * Set the value for this consuming attribute to <code>value</code>.
     *
     * @param value value for this consuming attribute
     */
    void setValue(E value);
}
