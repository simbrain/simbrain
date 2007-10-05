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

package org.simbrain.network.nodes;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.actions.CopyAction;
import org.simbrain.network.actions.CutAction;
import org.simbrain.network.actions.DeleteAction;
import org.simbrain.network.actions.GroupAction;
import org.simbrain.network.actions.PasteAction;
import org.simbrain.network.actions.SetTextPropertiesAction;
import org.simbrain.network.actions.SetNeuronPropertiesAction;

import edu.umd.cs.piccolox.nodes.PStyledText;

/**
 * An editable text object.
 */
public class TextObject extends ScreenElement implements PropertyChangeListener {

    /** The text object. */
    private PStyledText ptext;

    /**
     * Construct text object at specified location.
     *
     * @param netPanel reference to networkPanel
     * @param ptext the styled text
     */
    public TextObject(final NetworkPanel netPanel, final PStyledText ptext) {
        super(netPanel);
        this.ptext = ptext;
        //ptext.setPickable(false); // otherwise the child rather than the parent is picked
        this.addChild(ptext);
        this.setBounds(ptext.getBounds());

        addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this);
    }

    /** @Override. */
    public boolean isSelectable() {
        // TODO Auto-generated method stub
        return true;
    }

    /** @Override. */
    public boolean showSelectionHandle() {
        // TODO Auto-generated method stub
        return true;
    }

    /** @Override. */
    public boolean isDraggable() {
        // TODO Auto-generated method stub
        return true;
    }

    /** @Override. */
    protected boolean hasToolTipText() {
        // TODO Auto-generated method stub
        return false;
    }

    /** @Override. */
    protected String getToolTipText() {
        // TODO Auto-generated method stub
        return null;
    }

    /** @Override. */
    protected boolean hasContextMenu() {
        // TODO Auto-generated method stub
        return true;
    }

    /** @Override. */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();

        contextMenu.add(new CutAction(getNetworkPanel()));
        contextMenu.add(new CopyAction(getNetworkPanel()));
        contextMenu.add(new PasteAction(getNetworkPanel()));
        contextMenu.addSeparator();

        contextMenu.add(new GroupAction(getNetworkPanel()));
        contextMenu.addSeparator();

        contextMenu.add(new DeleteAction(getNetworkPanel()));
        contextMenu.addSeparator();

        contextMenu.add(new SetTextPropertiesAction(getNetworkPanel()));

        return contextMenu;

  }

    /** @Override. */
    protected boolean hasPropertyDialog() {
        return false;
    }

    /** @Override. */
    protected JDialog getPropertyDialog() {
        // TODO Auto-generated method stub
        return null;
    }

    /** @Override. */
    public void resetColors() {
        // TODO Auto-generated method stub
    }

    public void propertyChange(PropertyChangeEvent arg0) {
    setBounds(ptext.getBounds());
    }

    /**
     * @return the ptext
     */
    public PStyledText getPtext() {
        return ptext;
    }

    /**
     * @param ptext the ptext to set
     */
    public void setPtext(PStyledText ptext) {
        this.ptext = ptext;
    }
}
