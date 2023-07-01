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

import org.simbrain.network.gui.dialogs.NetworkPreferences;
import org.simbrain.util.ResourceManager;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Typesafe enumeration of edit modes.
 */
public final class EditMode {

    /**
     * Cursor center point.
     */
    private static final Point CENTER_POINT = new Point(9, 9);

    /**
     * Cursor for this edit mode.
     */
    private Cursor cursor;

    /**
     * Selection edit mode.
     */
    public static final EditMode SELECTION = new EditMode("selection", "menu_icons/Arrow.png");

    /**
     * Text edit mode.
     */
    public static final EditMode TEXT = new EditMode("text", "menu_icons/Text.png");

    /**
     * Wand mode mode.
     */
    public static final EditMode WAND = new EditMode("wand");

    /**
     * The image used for the cursor.
     */
    private BufferedImage cursorImage;

    /**
     * Create a new edit mode with the specified name.
     *
     * @param name       name of this edit mode
     * @param cursorName cursor name for this edit mode
     */
    private EditMode(final String name, final String cursorName) {

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = ResourceManager.getImage(cursorName);
        if (name.equals("selection")) {
            this.cursor = Cursor.getDefaultCursor();
        } else {
            this.cursor = toolkit.createCustomCursor(image, CENTER_POINT, name);
        }
    }

    /**
     * Construct a "wand" edit mode. TODO: Is this the right place for this?
     *
     * @param name   name of edit mode
     */
    private EditMode(String name) {
        resetWandCursor();
    }

    /**
     * Return the cursor for this edit mode.
     *
     * @return the cursor for this edit mode
     */
    public Cursor getCursor() {
        return cursor;
    }

    /**
     * Return true if this edit mode is <code>SELECTION</code>.
     *
     * @return true if this edit mode is <code>SELECTION</code>
     */
    public boolean isSelection() {
        return (this == SELECTION);
    }


    /**
     * Return true if this edit mode is <code>TEXT</code>.
     *
     * @return true if this edit mode is <code>TEXT</code>
     */
    public boolean isText() {
        return (this == TEXT);
    }

    /**
     * Return true if this edit mode is <code>WAND</code>.
     *
     * @return true if this edit mode is <code>WAND</code>
     */
    public boolean isWand() {
        return (this == WAND);
    }

    /**
     * Reset the wand cursor (must happen when its size is reset).
     */
    public void resetWandCursor() {
        cursorImage = new BufferedImage(NetworkPreferences.INSTANCE.getWandRadius() + 1, NetworkPreferences.INSTANCE.getWandRadius() + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) cursorImage.getGraphics();

        // Draw stroke around wand
        int stroke = 1;
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(stroke));
        g2.drawOval(0, 0, NetworkPreferences.INSTANCE.getWandRadius(), NetworkPreferences.INSTANCE.getWandRadius());

        // Draw wand itself
        // Pure yellow is (255, 255,0) but this does not look good when transparent on a white background.
        // To get it to actually look yellow reduce the green component a bit.
        g2.setColor(new Color(255, 230, 0, 180));
        g2.setStroke(new BasicStroke(1));

        g2.fillOval(0, 0, NetworkPreferences.INSTANCE.getWandRadius(), NetworkPreferences.INSTANCE.getWandRadius());

        Toolkit tk = Toolkit.getDefaultToolkit();
        Cursor newCursor = tk.createCustomCursor(cursorImage, CENTER_POINT, "wand");
        this.cursor = newCursor;
    }

}