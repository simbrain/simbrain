package org.simbrain.workspace;

import java.lang.reflect.Type;

public interface Attribute {

    /**
     * Return the name of this consuming attribute.
     *
     * @return the name of this consuming attribute
     */
    String getAttributeDescription();

    /**
     * returns the type of the generic parameter
     * 
     * @return the type of the generic parameter
     */
    Type getType();
}
