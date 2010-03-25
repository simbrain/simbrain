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
     * Returns the key for this attribute. This key serves two purposes.
     * 
     * First, it is used for serialization, as a String id for a given Attribute.
     * Because of this, NOTE, the key must be unique relative to other
     * Attributes in an AttributeHolder.
     * 
     * Second, this is used as a String description of the attribute, which is
     * used at various places in the GUI. So, the key value should be
     * human-readable.
     * 
     * Also note that the key of an attribute must be exactly the same after
     * deserialization as it was before serialization, even if the description
     * of the attribute was not serialized.
     * 
     * @return the key for this attribute.
     */
    String getKey();
    
    /**
     * Returns a reference the AttributeHolder which holds this Attribute.

     * @return parent AttributeHolder
     */
    AttributeHolder getParent();
}
