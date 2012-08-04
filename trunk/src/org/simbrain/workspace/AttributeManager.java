/*
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * The main usage of this class by API users is to create potential attributes
 * (PotentialConsumer and PotentialProducer), which are in turn used to create
 * couplings.
 * <p>
 * Most coupling creation should occur by way of the createPotential... methods
 * in this class. The preferred method is to create a potential attribute
 * (potential consumer or producer) which is then used to make a coupling.
 * <p>
 * A potential consumer or producer can be created in one of three ways
 * <ol>
 * <li>Using an <code>AttributeType</code> object.</li>
 * <li>Using a parent object, a method name, and a data type.</li>
 * <li>Using auxiliary arguments.</li>
 * </ol>
 * <p>
 * These concepts are discussed in the javadocs for <code>Attribute</code>.
 *
 * @author jyoshimi
 *
 * @see Attribute
 * @see AttributeType
 * @see PotentialAttribute
 * @see Coupling
 */
public class AttributeManager {

    /** Reference to parent component. */
    private WorkspaceComponent parentComponent;

    /**
     * @param parentWorkspace
     */
    public AttributeManager(WorkspaceComponent parentComponent) {
        this.parentComponent = parentComponent;
    }

    /**
     * Create a producer based on an argument. This version of the method does
     * the real work; others forward to it.
     *
     * @param parentObject base object
     * @param methodName name of method
     * @param dataType main data type of data (used in coupling manager)
     * @param argumentDataTypes data types of all arguments to the method
     * @param argumentValues arguments to the method
     * @param description description of the producer
     * @return the resulting producer
     */
    protected Producer<?> createProducer(final Object parentObject,
            final String methodName, final Class<?> dataType,
            final Class<?>[] argumentDataTypes, final Object[] argumentValues,
            final String description) {

        Producer<?> producer = new Producer() {

            private Method theMethod;

            // Static initializer
            {
                try {
                    if (argumentDataTypes == null) {
                        theMethod = parentObject.getClass().getMethod(
                                methodName);
                    } else {
                        theMethod = parentObject.getClass().getMethod(
                                methodName, argumentDataTypes);
                    }
                } catch (SecurityException e1) {
                    e1.printStackTrace();
                } catch (NoSuchMethodException e1) {
                    System.err.println("Could not find method " + methodName
                            + " with return type of "
                            + dataType.getCanonicalName());
                    e1.printStackTrace();
                }

            }

            /**
             * {@inheritDoc}
             */
            public Object getValue() {
                try {
                    return theMethod.invoke(parentObject, argumentValues);
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return null;
            }

            /**
             * {@inheritDoc}
             */
            public WorkspaceComponent getParentComponent() {
                return parentComponent;
            }

            /**
             * {@inheritDoc}
             */
            public Object getBaseObject() {
                return parentObject;
            }

            /**
             * {@inheritDoc}
             */
            public String getMethodName() {
                return methodName;
            }

            /**
             * {@inheritDoc}
             */
            public Class<?> getDataType() {
                return dataType;
            }

            /**
             * {@inheritDoc}
             */
            public Class<?>[] getArgumentDataTypes() {
                return argumentDataTypes;
            }

            /**
             * {@inheritDoc}
             */
            public Object[] getArgumentValues() {
                return argumentValues;
            }

            /**
             * {@inheritDoc}
             */
            public String getDescription() {
                return description;
            }

        };
        return producer;

    }

    /**
     * Create an actual producer from a potential producer.
     *
     * @param potentialAttribute the potential attribute to actualize
     * @return the resulting producer
     */
    protected Producer<?> createProducer(
            final PotentialAttribute potentialAttribute) {

        return createProducer(potentialAttribute.getBaseObject(),
                potentialAttribute.getMethodName(),
                potentialAttribute.getDataType(),
                potentialAttribute.getArgumentDataTypes(),
                potentialAttribute.getArgumentValues(),
                potentialAttribute.getDescription());
    }

    /**
     * Create a consumer. This version of the method does the real work; others
     * forward to it. Note that, unlike createProducer, this does not have a
     * main datatype argument, because that is implicitly the first element of
     * argument dataTypes. For more information on these fields see the
     * documentation for <code>Attribute</code>.
     *
     * @param parentObject parent object
     * @param methodName name of method
     * @param argumentDataTypes type of all arguments, where the first is the
     *            main data type
     * @param argumentValues values for auxiliary arguments
     * @param description description
     * @return the resulting consumer
     */
    protected Consumer<?> createConsumer(final Object parentObject,
            final String methodName, final Class<?>[] argumentDataTypes,
            final Object[] argumentValues, final String description) {

        Consumer<?> consumer = new Consumer() {

            private Method theMethod;

            // Static initializer
            {
                // System.out.println(Arrays.asList(argumentDataTypes));
                try {
                    theMethod = parentObject.getClass().getMethod(methodName,
                            argumentDataTypes);
                } catch (SecurityException e1) {
                    e1.printStackTrace();
                } catch (NoSuchMethodException e1) {
                    System.err.print("Could not find method " + methodName
                            + " ");
                    if (argumentDataTypes != null) {
                        System.err.print("with arguments of type ");
                        for (Class<?> type : argumentDataTypes) {
                            System.err.print(type.getCanonicalName());
                        }
                        System.err.println();
                    }
                    e1.printStackTrace();
                }
            }

            /**
             * {@inheritDoc}
             */
            public void setValue(Object value) {
                try {

                    if (argumentDataTypes.length == 1) {
                        theMethod.invoke(parentObject, new Object[] { value });
                    } else {
                        Object[] concatArray = new Object[argumentValues.length + 1];
                        concatArray[0] = value;
                        System.arraycopy(argumentValues, 0, concatArray, 1,
                                argumentValues.length);
                        theMethod.invoke(parentObject, concatArray);
                    }
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            /**
             * {@inheritDoc}
             */
            public WorkspaceComponent getParentComponent() {
                return parentComponent;
            }

            /**
             * {@inheritDoc}
             */
            public Object getBaseObject() {
                return parentObject;
            }

            /**
             * {@inheritDoc}
             */
            public String getMethodName() {
                return methodName;
            }

            /**
             * {@inheritDoc}
             */
            public Class<?>[] getArgumentDataTypes() {
                return argumentDataTypes;
            }

            /**
             * {@inheritDoc}
             */
            public Object[] getArgumentValues() {
                return argumentValues;
            }

            /**
             * {@inheritDoc}
             */
            public String getDescription() {
                return description;
            }

            /**
             * Assume first argument datatype is the "main" data type.
             */
            public Class<?> getDataType() {
                return argumentDataTypes[0];
            }

        };
        return consumer;

    }

    /**
     * Create an actual consumer from a potential consumer.
     *
     * @param potentialAttribute the potential attribute to actualize
     * @return the resulting consumer
     */
    protected Consumer<?> createConsumer(
            final PotentialAttribute potentialAttribute) {
        return createConsumer(potentialAttribute.getBaseObject(),
                potentialAttribute.getMethodName(),
                potentialAttribute.getArgumentDataTypes(),
                potentialAttribute.getArgumentValues(),
                potentialAttribute.getDescription());
    }

    // ////////////////////////////////////////
    // POTENTIAL ATTRIBUTE CREATION METHODS //
    // ////////////////////////////////////////

    /**
     * Create a potential producer using auxiliary arguments. For more
     * information on these fields see the javadocs for <code>Attribute</code>.
     *
     *
     * @param baseObject base object
     * @param methodName name of method on base object
     * @param dataType main data type. Type of data returned by the base object.
     * @param argDataTypes signature of method (new Class[] {Class1,
     *            Class2,...})
     * @param argValues values to pass to method (new Object[] {Object1,
     *            Object2,...})
     * @return the resulting producer
     */
    public PotentialProducer createPotentialProducer(Object baseObject,
            String methodName, Class<?> dataType, Class<?>[] argDataTypes,
            Object[] argValues) {
        String description = getDescriptionString(baseObject, methodName,
                dataType);
        return new PotentialProducer(parentComponent, baseObject, methodName,
                dataType, argDataTypes, argValues, description);
    }

    /**
     * Create a potential producer (without auxiliary arguments). This is
     * probably the main method to use in a script.
     *
     * @param baseObject base object
     * @param methodName name of method on base object
     * @param dataType main data type. Type of data returned by the base object.
     * @return the resulting producer
     */
    public PotentialProducer createPotentialProducer(final Object baseObject,
            final String methodName, final Class<?> dataType) {
        String description = getDescriptionString(baseObject, methodName,
                dataType);
        return new PotentialProducer(parentComponent, baseObject, methodName,
                dataType, null, null, description);
    }

    /**
     * Create a potential producer using an attribute type.
     *
     * @param baseObject the base object
     * @param type the attribute type
     * @return the potential producer
     */
    public PotentialProducer createPotentialProducer(final Object baseObject,
            final AttributeType type) {
        String methodName = type.getMethodName();
        Class<?> dataType = type.getDataType();
        PotentialProducer producer = createPotentialProducer(baseObject,
                methodName, dataType);
        String description = type.getBaseDescription();
        producer.setCustomDescription(description);
        return producer;
    }

    /**
     * Create a potential consumer using auxiliary arguments. For more
     * information on these fields see the javadocs for <code>Attribute</code>.
     *
     * @param baseObject base object
     * @param methodName name of method on base object
     * @param argDataTypes signature of method (new Class[] {datatype, Class1,
     *            Class2,...})
     * @param argValues values to pass to method (new Object[] {Class1,
     *            Class2,...})
     * @return the resulting consumer
     */
    public PotentialConsumer createPotentialConsumer(final Object baseObject,
            final String methodName, final Class<?>[] argDataTypes,
            final Object[] argValues) {
        Class<?> dataType = argDataTypes[0];
        String description = getDescriptionString(baseObject, methodName,
                dataType);
        return new PotentialConsumer(parentComponent, baseObject, methodName,
                argDataTypes, argValues, description);
    }

    /**
     * Create a potential consumer without auxiliary arguments. This is probably
     * the main method to use in a script.
     *
     * @param baseObject base object
     * @param methodName name of method on base object
     * @param dataType main data type. Type of data returned by the base object.
     * @return the resulting consumer
     */
    public PotentialConsumer createPotentialConsumer(final Object baseObject,
            final String methodName, final Class<?> dataType) {
        String description = getDescriptionString(baseObject, methodName,
                dataType);
        return new PotentialConsumer(parentComponent, baseObject, methodName,
                new Class<?>[] { dataType }, null, description);
    }

    /**
     * Create a potential consumer using an attribute type.
     *
     * @param baseObject the base object
     * @param type the attribute type
     * @return the potential consumer
     */
    public PotentialConsumer createPotentialConsumer(final Object baseObject,
            final AttributeType type) {
        String methodName = type.getMethodName();
        Class<?> dataType = type.getDataType();
        PotentialConsumer consumer = createPotentialConsumer(baseObject,
                methodName, dataType);
        String description = type.getBaseDescription();
        consumer.setCustomDescription(description);
        return consumer;
    }

    /**
     * Returns a formatted description string. E.g "Neuron:activatoin<double>".
     *
     * @param baseObject base object
     * @param methodName base name of method
     * @param dataType class of data
     * @return formatted string
     */
    private String getDescriptionString(Object baseObject, String methodName,
            Class<?> dataType) {
        return baseObject.getClass().getSimpleName() + ":" + methodName + "<"
                + dataType.getSimpleName() + ">";
    }
}
