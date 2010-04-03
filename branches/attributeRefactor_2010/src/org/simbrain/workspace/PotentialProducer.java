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

import java.lang.reflect.InvocationTargetException;

/**
 * TODO
 * 
 * @author jyoshimi
 *
 * @param <E>
 */
public class PotentialProducer<E> extends PotentialAttribute {

    public PotentialProducer(AttributeType type, WorkspaceComponent parent,
            Object parentObject, String methodName) {
        super(type, parent, parentObject, methodName);
    }

    /**
     *  Instantiate! Entelechy!
     *
     * @return
     */
    public Producer<E> actualize() {
        //Type type = new Type(this.getType().getDataType());
        Producer<E> consumer = new Producer<E>() {

            // Static initializer
            {
                try {
                    theMethod = getParentObject().getClass().getMethod(
                            getMethodName(), null);
                } catch (SecurityException e1) {
                    e1.printStackTrace();
                } catch (NoSuchMethodException e1) {
                    System.out.println("Could not find method " + getMethodName()
                            + " in class "
                            + getParentObject().getClass().getCanonicalName());
                    e1.printStackTrace();
                }
            }
            public E getValue() {
                try {
                    return (E) theMethod.invoke(getParentObject(), null);
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

            // TODO: Below can become simplified to getType?  Or otherwise moved...
            public String getDescription() {
                return PotentialProducer.this.getDescription();
            }

            public WorkspaceComponent getParentComponent() {
                return PotentialProducer.this.getParent();
            }
        };
        return consumer;

    }



}