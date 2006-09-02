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
package org.simbrain.world.dataworld;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

/**
 * The <b>ButtonEditor</b> class provides an editor for buttons that allows you to change their names.
 *
 * @author RJB
 */
public class ButtonEditor extends AbstractCellEditor implements TableCellEditor, KeyListener {

    /** Text field for this button editor. */
    private JTextField text = new JTextField();


    /**
     * Create a new button editor for the specified data world.
     *
     * @param dataWorld data world
     */
    public ButtonEditor(final DataWorld dataWorld) {
        text.addKeyListener(this);
    }


    /** @see javax.swing.AbstractCellEditor */
    public Object getCellEditorValue() {
        DataWorld.editButtons = false;

        return new JButton(text.getText());
    }

    /** @see TableCellEditor */
    public Component getTableCellEditorComponent(final JTable table,
            final Object value, final boolean isSelected, final int row,
            final int column) {

        text.setText(((JButton) value).getText());

        return text;
    }

    /** @see KeyListener */
    public void keyTyped(final KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
            fireEditingStopped();
        } else if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
            fireEditingCanceled();
        }
    }

    /** @see KeyListener */
    public void keyPressed(final KeyEvent keyEvent) {
        // empty
    }

    /** @see KeyListener */
    public void keyReleased(final KeyEvent keyEvent) {
        // empty
    }
}
