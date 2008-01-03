package org.simbrain.workspace;

import java.lang.reflect.Type;

/**
 * Defines the base API for consuming and producing attributes.
 * 
 * @author Matt Watson
 */
public interface Attribute {

    /**
     * Return the name of this consuming attribute.
     *
     * @return the name of this consuming attribute.
     */
    String getAttributeDescription();

    /**
     * returns the type of the generic parameter.
     * 
     * @return the type of the generic parameter.
     */
    Type getType();
}
