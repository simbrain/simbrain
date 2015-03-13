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
 * Defines the base API for consumers and producers.
 * <p>
 * An attribute involves 6 objects, described below, which are used to secure
 * reference to a method (using reflection) which either produces or consumes
 * data of type E (see the <code>Coupling</code> documentation). From this
 * standpoint, a coupling is a pair consisting of a getter and a setter:
 * <p>
 * Producer (getX) -- E ---> Consumer (setX).
 * <p>
 * Usually (1)-(3) below are all that are needed to create an attribute. (4) Can
 * be customized as needed. (5) and (6) are only used in special cases where
 * "auxiliary" arguments are needed. These fields are described here. However in
 * practice, an attribute is created by way of a <code>PotentialAttribute</code>
 * , using the <code>AttributeManager</code>.
 * <p>
 * (1) BaseObject (Object): This is the basic object (a neuron, an agent in a
 * simulated world, etc) where the method lives.
 * <p>
 * (2) MethodName (String): The name of the method in question. Usually a getter
 * for producers, and a setter for consumers.
 * <p>
 * (3) DataType (Class): The "main" data type (usually double) of the data
 * produced or consumed by a coupling. Note that <code>Producer</code>,
 * <code>Consumer</code>, and <code>Coupling</code> are all parameterized by
 * this type.
 * <p>
 * <ul>
 * <li>Producers: this is the return type of a getter.</li>
 * <li>Consumers: this is the type of the first argument of a setter.</li>
 * </ul>
 * <p>
 * (4) Description (String): A description of the attribute, which is useful in
 * displaying couplings, and also in displaying a <code>PotentialAttibute</code>
 * in the GUI, and also for debugging. Can be set to a custom value. Otherwise a
 * default description describing the object, methodname, and datatype is made.
 * <p>
 * Auxiliary arguments are used to secure access to an underlying object that
 * produces or consumes a value.
 * <p>
 * <ul>
 * <li>Producer example: to get one component of a smell vector, we need to call
 * "getSmell(int index)". So the getter has one integer index argument. The main
 * datatype corresponds to the return type.</li>
 * <li>Consumer example: to access a particular dimension of a barchart, we call
 * a "setValue(value, index) function. So we need to pass in an index,
 * corresponding to a bar. Note that the first argument is the datatype for the
 * consumer, and that consumers with auxiliary arguments must have the main
 * datatype as their first argument.</li>
 * </ul>
 * <p>
 * (5) ArgumentDataTypes (Class[]): The method signature of a getter or setter
 * that requires auxiliary arguments. Usually null unless auxiliary arguments
 * are needed.
 * <p>
 * (6) ArgumentValues (Object[]): The values passed to a getter or setter with
 * auxiliary arguments.Usually null unless auxiliary arguments are needed.
 * <p>
 * The way auxiliary arguments work is a little different for producers and
 * consumers
 * <p>
 * Producers: These are straightforward. The method signature for the getter and
 * the values passed in line up.
 * <p>
 * <ul>
 * <li>ArgumentDataType: (Class1, Class2,...)</li>
 * <li>ArgumentValues: (Object1, Object2,...)</li>
 * </ul>
 * <p>
 * Consumers: This is a bit more complex, because the method signature and
 * values are offset by 1. This is because the fist item of the method signature
 * vector is the datatype. Note that this constrains what kind of setter can be
 * a consumer with auxiliary arguments: in setters like this the first argument
 * must be the same as the DataType.
 * <p>
 * <ul>
 * <li>ArgumentDataType: (DataType, Class1, Class2,...)</li>
 * <li>ArgumentValues: (Object1, Object2,...)</li>
 * </ul>
 * <p>
 * Finally, note that the actual act of creating a producer or consumer using
 * the reflection API happens in the AttributeManager class.
 *
 * @author Matt Watson
 * @author Jeff Yoshimi
 *
 * @see AttributeManager
 * @see PotentialAttribute
 * @see Coupling
 */
public interface Attribute {

    /**
     * Returns a reference to the parent component.
     *
     * @return parent parent component
     */
    public WorkspaceComponent getParentComponent();

    /**
     * Returns the base object associated with this attribute.
     *
     * @return the base object.
     */
    public Object getBaseObject();

    /**
     * Returns the "main" data type of this attribute (see class javadocs
     * above).
     *
     * @return the data type, as a class.
     */
    public Class<?> getDataType();

    /**
     * Returns the name of the method associated with this attribute.
     *
     * @return base method name
     */
    public String getMethodName();

    /**
     * Returns the method signature for auxiliary arguments (see the javadocs
     * above for more information on this).
     *
     * @return the argumentDataTypes
     */
    public Class<?>[] getArgumentDataTypes();

    /**
     * Returns the argument values auxiliary arguments (see the javadocs above
     * for more information on this).
     *
     * @return the argumentValues
     */
    public Object[] getArgumentValues();

    /**
     * Returns a descriptive name for this attribute.
     *
     * @return the name of this attribute.
     */
    public String getDescription();
}
