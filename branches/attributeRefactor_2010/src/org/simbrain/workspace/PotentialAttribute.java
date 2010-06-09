/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.workspace;

/**
 * Objects of this class contain everything necessary to produce a particular
 * consumer or producer, as well as information used in displaying the potential
 * attribute (for GUI elements which manage coupling creation).
 *
 * @author jyoshimi
 */
public class PotentialAttribute {

    /** Parent workspace component. */
    private WorkspaceComponent parent;

    /**
     * A string which can be used to obtain a reference to the object. Also used
     * by getDescription()
     */
    private String objectKey;

    /** Potential producing or consuming object. */
    private Object baseObject;

    /** Base name of the method that sets or gets the attribute. */
    private String methodBaseName;

    /** The data type (double, string, etc) of a consumer or producer. */
    private Class dataType;

    /**
     * Construct a potential attribute.
     *
     * @param parent parent workspace component
     * @param objectKey string key or object
     * @param methodBaseName method name
     * @param dataType class of data
     */
    public PotentialAttribute(WorkspaceComponent parent, String objectKey, Object object,
            String methodBaseName, Class dataType) {
        this.parent = parent;
        this.objectKey = objectKey;
        this.baseObject = object;
        this.methodBaseName = methodBaseName;
        this.dataType = dataType;
    }

    /**
     * @param parent
     * @param objectKey
     * @param dataType
     */
    public PotentialAttribute(WorkspaceComponent parent, String objectName, Object object, AttributeType type) {
        this.parent = parent;
        this.objectKey = objectName;
        this.baseObject = object;
        this.methodBaseName = type.getAttributeName();
        this.dataType = type.getDataType();
    }

    /**
     * Actualize this potential attribute into a producer.
     *
     * @return the producer corresponding to this potential attribute.
     */
    public Producer createProducer() {
        return parent.getProducer(this);
    }

    /**
     * Actualize this potential attribute into a consumer.
     *
     * @return the consumer corresponding to this potential attribute.
     */
    public Consumer createConsumer() {
        return parent.getConsumer(this);
    }

    /**
     * Returns a description of this potential attribute; used in GUI.
     *
     * @param objectKey
     * @param type
     * @return
     */
    public String getDescription() {
        return objectKey + ":" + methodBaseName + "<"
                + dataType.getCanonicalName() + ">";
    }

    /**
     * @return the parent
     */
    public WorkspaceComponent getParent() {
        return parent;
    }

    /**
     * @return the objectKey
     */
    public String getObjectKey() {
        return objectKey;
    }

    /**
     * @return the methodName
     */
    public String getMethodBaseName() {
        return methodBaseName;
    }

    /**
     * @return the attributeType
     */
    public Class<?> getDataType() {
        return dataType;
    }

    /**
     * @return the object
     */
    public Object getBaseObject() {
        return baseObject;
    }

}
