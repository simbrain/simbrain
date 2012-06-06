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

/**
 * A raft of methods for creating attributes and potential attributes. For
 * attributes (Producer and Consumer) the main creation method, using reflection
 * is here.
 *
 * You can create an attribute without specifying a description, in which case a
 * standard description is created. For potential attributes (PotentialProducer
 * and PotentialConsumer), there are two choices, so that there are four
 * creation methods. As with attributes, you can specify a custom description or
 * not. You can also use an attribute type when creating the potential
 * attribute.
 *
 * Main type. The type used when passing info from producer to consumer. These
 * must match.
 *
 * Auxiliary arguments. Used when calling the method (using reflection)
 * associated with a producer or consumer. In these cases both the argument
 * types and an unchanging argument value must be specified.
 *
 * For producers (getters): the return type is the main type, and all args are
 * additional. Example: double getValue(int bar-index). double is the main type
 * int is auxiliary
 *
 * For consumers (setters): the _first_ argument is the main type, and all other
 * arguments are auxiliary.
 *
 * TODO: Refer other classes here for info
 *
 * @author jyoshimi
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
    public Producer<?> createProducer(final Object parentObject,
            final String methodName, final Class<?> dataType,
            final Class<?>[] argumentDataTypes, final Object[] argumentValues,
            final String description) {

        Producer<?> producer = new Producer() {

            private Method theMethod;

            // Static initializer
            {
                try {
                    theMethod = parentObject.getClass().getMethod(methodName,
                            argumentDataTypes);
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
     * Create a producer based on a method with no arguments.
     *
     * @param parentObject base object
     * @param methodName name of method
     * @param dataType type of data
     * @param description description
     * @return the resulting producer
     */
    public Producer<?> createProducer(final Object parentObject,
            final String methodName, final Class<?> dataType,
            final String description) {

        return createProducer(parentObject, methodName, dataType,
                (Class[]) null, (Object[]) null, description);
    }

    /**
     * Create a producer based on a method with no arguments.
     *
     * @param parentObject base object
     * @param methodName name of method
     * @param dataType type of data
     * @param description description
     * @return the resulting producer
     */
    public Producer<?> createProducer(final Object parentObject,
            final String methodName, final Class<?> dataType,
            final Class<?> argument, final Object value,
            final String description) {

        return createProducer(parentObject, methodName, dataType,
                new Class[] { argument }, new Object[] { value }, description);
    }

    /**
     * Create a producer without specifying a custom description (the
     * description is created automatically).
     *
     * @param baseObject base object
     * @param methodName method name
     * @param dataType data type
     * @return created producer
     */
    public Producer<?> createProducer(final Object baseObject,
            final String methodName, final Class<?> dataType) {
        String description = getDescriptionString(baseObject, methodName,
                dataType);
        return createProducer(baseObject, methodName, dataType, description);
    }

    /**
     * Create an actual producer from a potential producer.
     *
     * @param potentialAttribute the potential attribute to actualize
     * @return the resulting producer
     */
    public Producer<?> createProducer(
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
     * argument dataTypes.
     *
     * TODO: Currently only works for one or two arguments only.
     *
     * @param parentObject parent object
     * @param methodName name of method
     * @param argumentDataTypes type of all arguments, where the first is the
     *            main data type
     * @param argumentValues values for arguments _after_ the first argument
     * @param description description
     * @return the resulting consumer
     */
    public Consumer<?> createConsumer(final Object parentObject,
            final String methodName, final Class<?>[] argumentDataTypes,
            final Object[] argumentValues, final String description) {

        Consumer<?> consumer = new Consumer() {

            Method theMethod;

            // Static initializer
            {
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
                    // Currently only works for case of one or two arguments.
                    if (argumentDataTypes.length == 1) {
                        theMethod.invoke(parentObject, new Object[] { value });
                    } else {
                        theMethod.invoke(parentObject, new Object[] { value,
                                argumentValues[0] });
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
     * Create a consumer using: 1) Parent Object 2) Method name 3) Data type (so
     * no auxiliary args: one value in argument list and null value list)
     * Description is automatically created.
     */
    public Consumer<?> createConsumer(final Object baseObject,
            final String methodName, final Class<?> dataType) {
        String description = getDescriptionString(baseObject, methodName,
                dataType);
        return createConsumer(baseObject, methodName,
                new Class<?>[] { dataType }, null, description);
    }

    /**
     * Create a consumer using: 1) Parent Object 2) Method name 3) Data type (so
     * no auxiliary args: one value in argument list and null value list) 4)
     * Description
     */
    public Consumer<?> createConsumer(final Object baseObject,
            final String methodName, final Class<?> dataType,
            final String description) {
        return createConsumer(baseObject, methodName,
                new Class<?>[] { dataType }, null, description);
    }

    /**
     * Create an actual consumer from a potential consumer.
     *
     * @param potentialAttribute the potential attribute to actualize
     * @return the resulting consumer
     */
    public Consumer<?> createConsumer(
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
     * Create a potential producer. All information is provided.
     *
     * @param parentObject base object
     * @param methodName name of method
     * @param dataType type of data
     * @param description custom description
     * @return the resulting producer
     */
    public PotentialProducer createPotentialProducer(final Object parentObject,
            final String methodName, final Class<?> dataType,
            final String description) {
        return new PotentialProducer(parentComponent, parentObject, methodName,
                dataType, description);
    }

    /**
     * Create a potential producer. The description is automatically generated.
     *
     * @param baseObject base object
     * @param methodName name of method
     * @param dataType type of data
     * @return the resulting producer
     */
    public PotentialProducer createPotentialProducer(final Object baseObject,
            final String methodName, final Class<?> dataType) {
        return createPotentialProducer(baseObject, methodName, dataType,
                getDescriptionString(baseObject, methodName, dataType));
    }

    /**
     * Returns a potential producer with a base object and attribute type. The
     * description string is automatically generated.
     *
     * @param baseObject the base object
     * @param type the attribute type
     * @return the potential producer
     */
    public PotentialProducer createPotentialProducer(final Object baseObject,
            final AttributeType type) {
        return createPotentialProducer(baseObject, type,
                type.getBaseDescription());
    }

    /**
     * Returns a potential producer with a base object and attribute type. A
     * custom description is provided
     *
     * @param baseObject the base object
     * @param type the attribute type
     * @param description the custom description
     * @return the potential producer
     */
    public PotentialProducer createPotentialProducer(final Object baseObject,
            final AttributeType type, final String description) {
        String methodName = type.getMethodName();
        Class<?> dataType = type.getDataType();
        return createPotentialProducer(baseObject, methodName, dataType,
                description);
    }

    // Potential Consumers

    /**
     * Create a potential consumer. All information is provided.
     *
     * @param parentObject base object
     * @param methodName name of method
     * @param dataType type of data (so no auxiliary args: one value in argument
     *            list and null value list)
     * @param description custom description
     * @return the resulting consumer
     */
    public PotentialConsumer createPotentialConsumer(final Object parentObject,
            final String methodName, final Class<?> dataType,
            final String description) {
        return new PotentialConsumer(parentComponent, parentObject, methodName,
                new Class<?>[] { dataType }, null, description);
    }

    /**
     * Create a potential consumer. The description is automatically generated.
     *
     * @param baseObject base object
     * @param methodName name of method
     * @param dataType type of data
     * @return the resulting consumer
     */
    public PotentialConsumer createPotentialConsumer(final Object baseObject,
            final String methodName, final Class<?> dataType) {
        return createPotentialConsumer(baseObject, methodName, dataType,
                getDescriptionString(baseObject, methodName, dataType));
    }

    /**
     * Returns a potential consumer with a base object and attribute type. The
     * description string is automatically generated.
     *
     * @param baseObject the base object
     * @param type the attribute type
     * @return the potential consumer
     */
    public PotentialConsumer createPotentialConsumer(final Object baseObject,
            final AttributeType type) {
        return createPotentialConsumer(baseObject, type,
                type.getBaseDescription());
    }

    /**
     * Returns a potential consumer with a base object and attribute type. A
     * custom description is provided
     *
     * @param baseObject the base object
     * @param type the attribute type
     * @param description the custom description
     * @return the potential consumer
     */
    public PotentialConsumer createPotentialConsumer(final Object baseObject,
            final AttributeType type, final String description) {
        String methodName = type.getMethodName();
        Class<?> dataType = type.getDataType();
        return createPotentialConsumer(baseObject, methodName, dataType,
                description);
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
