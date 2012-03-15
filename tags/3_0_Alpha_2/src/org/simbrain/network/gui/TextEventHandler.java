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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;

import javax.swing.text.BadLocationException;

import org.simbrain.network.gui.nodes.TextNode;
import org.simbrain.network.interfaces.NetworkTextObject;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;
import edu.umd.cs.piccolox.event.PStyledTextEventHandler;
import edu.umd.cs.piccolox.nodes.PStyledText;

/**
 * Event handler for text nodes, so that they can be edited directly on the
 * screen.
 */
public class TextEventHandler extends PStyledTextEventHandler implements
        ActionListener {

    /** Reference to parent network. */
    private NetworkPanel networkPanel;

    /**
     * Construct text event handler.
     *
     * @param networkPanel reference to network panel.
     */
    public TextEventHandler(final NetworkPanel networkPanel) {
        super(networkPanel.getCanvas());
        this.networkPanel = networkPanel;
        this.setEventFilter(new TextEventFilter());
    }

    @Override
    public void mousePressed(final PInputEvent inputEvent) {

        PNode pickedNode = inputEvent.getPickedNode();
        stopEditing();

        // Start editing if this a text object
        if (pickedNode instanceof PStyledText) {
            this.reshapeEditorLater();
            startEditing(inputEvent, (PStyledText) pickedNode);
        }

        else if (pickedNode instanceof PCamera) {

            // Make a new text object and then edit it
            NetworkTextObject text = new NetworkTextObject(
                    networkPanel.getRootNetwork(), inputEvent.getPosition()
                            .getX(), inputEvent.getPosition().getY());
            networkPanel.getRootNetwork().addText(text);
            TextNode node = networkPanel.findTextNode(text);
            if (node != null) {
                startEditing(inputEvent, node.getPStyledText());
            }
        }
    }

    /**
     * Removes empty text objects.
     */
    void stopEditing() {
        if (editedText != null) {
            TextNode node = (TextNode) editedText.getParent();
            editedText.getDocument().removeDocumentListener(docListener);
            editedText.setEditing(false);
            if (editedText.getDocument().getLength() == 0) {
                networkPanel.getRootNetwork().deleteText(node.getTextObject());
                editedText.removeFromParent();
            } else {
                try {
                    node.getTextObject().setText(
                            editedText.getDocument().getText(0,
                                    editedText.getDocument().getLength()));
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
                node.update();
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
    private class TextEventFilter extends PInputEventFilter {

        /**
         * Create a new zoom event filter.
         */
        public TextEventFilter() {
            super(InputEvent.BUTTON1_MASK);
        }

        /** @see PInputEventFilter */
        public boolean acceptsEvent(final PInputEvent event, final int type) {
            EditMode editMode = networkPanel.getEditMode();
            return (editMode.isText() && super.acceptsEvent(event, type));
        }
    }

    /** @Override */
    public void actionPerformed(ActionEvent arg0) {
    }

}
