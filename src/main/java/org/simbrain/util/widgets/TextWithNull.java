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
