/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network;

import java.awt.Dimension;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * <b>Utils</b> provides static utility methods for the Network class
 */
public class NetworkUtils {

	/**
	 * Checks to see if all the objects in a list return the same
	 * value for a  method which the user provides by name. The method
	 * can have no arguments (typically a getter method). 
	 * 
	 * @param toCheck the list of objects to check for consistency
	 * @param methodName the method to be invoked (uses reflection) 
	 * @return true if the list of objects returns the same value for methodName, false otherwise
	 */
	public static boolean isConsistent(
		List toCheck,
		Class theClass,
		String methodName) {

		Method theMethod = null;
		try {
			theMethod = theClass.getMethod(methodName, null);
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
		}

		Iterator i = toCheck.iterator();
		while (i.hasNext()) {
			Object o1 = i.next();
			Object result1 = null;
			try {
				result1 = theMethod.invoke(o1, null);
			} catch (Exception e) {
				e.printStackTrace();
			}

			Iterator j = toCheck.iterator();
			while (j.hasNext()) {
				Object o2 = j.next();
				Object result2 = null;
				try {
					result2 = theMethod.invoke(o2, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (result1.equals(result2) == false) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Helper method for creating formatted rows in dialog boxes, which consist of a 
	 * label, a component, and some tool-tip for the label.
	 * 
	 * @param text the text to be displayed to the left of the commponent as a JLabel
	 * @param toolTip a tooltip / help to be displayed for the label
	 * @param theComponent the component to be displayed to the right of the label
	 * @return a JPanel containing the formatted label and component 
	 */
	public static JPanel createRow(String text, String toolTip, JComponent theComponent) {
		JPanel retPanel = new JPanel();
		retPanel.setLayout(new BoxLayout(retPanel, BoxLayout.X_AXIS));
		JLabel theLabel = new JLabel(text);
		theLabel.setToolTipText(toolTip);
		retPanel.add(theLabel);
		retPanel.add(Box.createHorizontalGlue());
		retPanel.add(Box.createRigidArea(new Dimension(8, 0)));
		retPanel.add(Box.createHorizontalGlue());
		if (theComponent instanceof JTextField) {
			((JTextField)theComponent).setCaretPosition(0);
		}
		theComponent.setMinimumSize(new Dimension(120, 22));
		theComponent.setPreferredSize(new Dimension(120, 22));
		theComponent.setMaximumSize(new Dimension(120, 22));
		retPanel.add(theComponent);
		return retPanel;
	}

}
