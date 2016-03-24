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
package org.simbrain.network.gui;

import java.awt.Dimension;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.util.ParameterGetter;

/**
 * <b>NetworkUtils</b> provides static utility methods for the Network class.
 */
public class NetworkUtils {

    /**
     * Reflection-based method to see if all the objects in a list return the
     * same value for a method which the user provides by name. The method can
     * have no arguments (typically a getter method).
     *
     * @param toCheck the list of objects to check for consistency
     * @param methodName the method to be invoked (uses reflection)
     * @param theClass the class method is in
     * @return true if the list of objects returns the same value for
     *         methodName, false otherwise
     */
    @SuppressWarnings("rawtypes")
    public static boolean isConsistent(final Collection toCheck,
            final Class<?> theClass, final String methodName) {
        Method theMethod = null;

        try {
            theMethod = theClass.getMethod(methodName, (Class[]) null);
        } catch (SecurityException e1) {
            e1.printStackTrace();
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        }

        if (toCheck.size() == 0) {
            throw new IllegalArgumentException("List to check is empty.");
        }

        Object o1 = toCheck.iterator().next();
        Object result1 = null;

        try {
            result1 = theMethod.invoke(o1, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Iterator<Object> j = toCheck.iterator();
        j.next();
        while (j.hasNext()) {
            Object o2 = j.next();
            Object result2 = null;
            try {
                result2 = theMethod.invoke(o2, (Object[]) null);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!result1.equals(result2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper method for creating formatted rows in dialog boxes, which consist
     * of a label, a component, and some tool-tip for the label.
     *
     * @param text
     *            the text to be displayed to the left of the commponent as a
     *            JLabel
     * @param toolTip
     *            a tooltip / help to be displayed for the label
     * @param theComponent
     *            the component to be displayed to the right of the label
     *
     * @return a JPanel containing the formatted label and component
     */
    public static JPanel createRow(final String text, final String toolTip,
            final JComponent theComponent) {
        JPanel retPanel = new JPanel();
        retPanel.setLayout(new BoxLayout(retPanel, BoxLayout.X_AXIS));

        JLabel theLabel = new JLabel(text);
        theLabel.setToolTipText(toolTip);
        retPanel.add(theLabel);
        retPanel.add(Box.createHorizontalGlue());
        retPanel.add(Box.createRigidArea(new Dimension(8, 0)));
        retPanel.add(Box.createHorizontalGlue());

        if (theComponent instanceof JTextField) {
            ((JTextField) theComponent).setCaretPosition(0);
        }

        theComponent.setMinimumSize(new Dimension(120, 22));
        theComponent.setPreferredSize(new Dimension(120, 22));
        theComponent.setMaximumSize(new Dimension(120, 22));
        retPanel.add(theComponent);

        return retPanel;
    }
    
    /**
     * Checks whether all the objects in a list return the same value for a
     * getter method. Not based on reflection and so it outperforms its
     * counterpart of the same name.
     * 
     * @param <O> the type of the source objects
     * @param <V> the value returned by the getter
     * @param sources the source objects to check
     * @param getter the getter on the source objects
     * @return true if the set of objects have the same getter value
     */
    public static <O, V> boolean isConsistent(Collection<O> sources,
    		ParameterGetter<O,V> getter) {
    	if (sources.size() <= 0) {
    		throw new IllegalArgumentException("Source list is empty.");
    	}
    	// TODO: Redo using stream
    	// TODO: Deal with mixed activity generator / neuron case
    	Iterator<O> sourceIter = sources.iterator();
    	O sourceFirst = sourceIter.next();
    	V val = getter.getParameter(sourceFirst);
    	while (sourceIter.hasNext()) {
    		if (!val.equals(getter.getParameter(sourceIter.next()))) {
    			// Found an inconsistency
    			return false;
    		}
    	}
    	// No inconsistencies were found
    	return true;
    }



}
