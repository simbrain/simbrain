/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;

import org.simbrain.network.nodes.TextObject;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;
import edu.umd.cs.piccolox.event.PStyledTextEventHandler;
import edu.umd.cs.piccolox.nodes.PStyledText;

/**
 * Text Event Handler.
 */
public class TextEventHandler extends PStyledTextEventHandler implements ActionListener {

    /** Reference to parent network. */
    private NetworkPanel net;

    /**
     *  Constructor.
     * @param canvas reference to network panel.
     */
    public TextEventHandler(final PCanvas canvas) {
        super(canvas);
        this.setEventFilter(new TextEventFilter());
        net = (NetworkPanel) canvas;
    }

    /** Builds a TextObject on mouse clicks. */
    public void mousePressed(final PInputEvent inputEvent) {
        PNode pickedNode = inputEvent.getPickedNode();
        stopEditing();
        if (pickedNode instanceof PStyledText) {
            startEditing(inputEvent, (PStyledText) pickedNode);
        }

        else if (pickedNode instanceof PCamera) {

            PStyledText newText = createText();
            TextObject textObj = new TextObject(net, newText);
            Insets pInsets = newText.getInsets();
            canvas.getLayer().addChild(textObj);
            textObj.translate(inputEvent.getPosition().getX() - pInsets.left,
                    inputEvent.getPosition().getY() - pInsets.top);
            startEditing(inputEvent, newText);

        }
    }

    /** Removes empty text objects. */
    public void stopEditing() {
        if (editedText != null) {
            editedText.getDocument().removeDocumentListener(docListener);
            editedText.setEditing(false);
            if (editedText.getDocument().getLength() == 0) {
                if (editedText.getParent() != null) {
                    editedText.getParent().removeFromParent();
                }
                editedText.removeFromParent();
            } else {
                editedText.syncWithDocument();
            }
            editor.setVisible(false);
            canvas.repaint();
            editedText = null;
        }

    }

    /**
     * Text event filter, accepts left mouse clicks, but only when the network
     * panel's edit mode is <code>EditMode.TEXT</code>.
     */
    private class TextEventFilter
        extends PInputEventFilter {


        /**
         * Create a new zoom event filter.
         */
        public TextEventFilter() {
            super(InputEvent.BUTTON1_MASK);
        }


        /** @see PInputEventFilter */
        public boolean acceptsEvent(final PInputEvent event, final int type) {

            NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
            EditMode editMode = networkPanel.getEditMode();

            return (editMode.isText() && super.acceptsEvent(event, type));
        }
    }

    /** @Override */
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub
        
    }

}
