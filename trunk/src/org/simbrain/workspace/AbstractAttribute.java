package org.simbrain.workspace;

public abstract class AbstractAttribute implements Attribute {

    /**
     * A simple implementation of attribute description that returns the key for
     * the attribute.
     */
    public String getAttributeDescription() {
        return this.getParent().getDescription() + ":" + getKey();
    }
}
