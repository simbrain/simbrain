package org.simbrain.workspace;

import java.lang.reflect.Type;

/**
 * Defines the base API for consuming and producing attributes.
 * 
 * @author Matt Watson
 */
public interface Attribute {

    /**
     * Returns the descriptive name of this attribute.
     * 
     * @return the name of this attribute.
     */
    String getAttributeDescription();

    /**
     * Returns the key for this attribute.
     * This is used for serialization and in interface
     * elements which display attributes.
     *
     * NOTE 1: This description must be unique relative to
     * other Attributes in an AttributeHolder.
     * 
     * NOTE 2: The key of the attribute must be exactly
     * the same after deserialization as it was before serialization,
     * even if the description of the attribute was not serialized.
     * 
     * @return the key for this attribute.
     */
    String getKey();
    
    /**
     * returns the type of the generic parameter.
     * 
     * @return the type of the generic parameter.
     */
    Type getType();
    
    /**
     * Returns a reference the AttributeHolder which holds this Attribute.

     * @return parent AttributeHolder
     */
    AttributeHolder getParent();
}
