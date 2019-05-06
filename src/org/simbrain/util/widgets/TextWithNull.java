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
package org.simbrain.util.widgets;

import org.simbrain.util.SimbrainConstants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * <b>TextWithNull</b> is a text field with a null state that represents
 * being in a "null state".  This is shown as a "..." but without relying on that
 * actual string sequence to represent the null state.
 * <br>
 * One minor issue addressed by this is that it facilitates use of "..." as a regular string
 * in this kind of text field.
 */
public class TextWithNull extends JTextField {

    /**
     * If true this text field is in the null state
     */
    boolean isNull = false;

    /**
     * Default constructor.
     */
    public TextWithNull() {
        super();

        // When someone types in this text field it is no longer considered to be in a
        // null state
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                isNull = false;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    /**
     * Whether this widget is in its null state
     */
    public boolean isNull() {
        return isNull;
    }

    /**
     * Put this widget in it's "null state", representing inconsistent values
     */
    public void setNull() {
        setText(SimbrainConstants.NULL_STRING); // this calls insertUpdate, which sets isNull to false
        isNull = true;
    }
}
