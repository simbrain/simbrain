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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import org.simbrain.resource.ResourceManager;

/**
 * Typesafe enumeration of edit modes.
 */
public final class EditMode {

    /** Cursor center point. */
    private static final Point CENTER_POINT = new Point(9, 9);

    /** Cursor for this edit mode. */
    private Cursor cursor;

    /** Selection edit mode. */
    public static final EditMode SELECTION = new EditMode("selection",
            "Arrow.png");

    /** Text edit mode. */
    public static final EditMode TEXT = new EditMode("text", "Text.png");

    /** Default wand radius. */
    private static final int DEFAULT_WAND_RADIUS = 40;

    /** Default wand radius. */
    private static final int MINIMUM_WAND_RADIUS = 15;

    /** Wand mode mode. */
    public static final EditMode WAND = new EditMode("wand",
            DEFAULT_WAND_RADIUS);

    /** Radius of wand in wand mode. */
    private static int wandRadius = DEFAULT_WAND_RADIUS;

    /** The image used for the cursor. */
    private BufferedImage cursorImage;

    /**
     * Create a new edit mode with the specified name.
     *
     * @param name name of this edit mode
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
     * @param name name of edit mode
     * @param radius radius of wand
     */
    private EditMode(String name, int radius) {
        this.wandRadius = radius;
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
     * @return the current wand radius
     */
    public static int getWandRadius() {
        return wandRadius;
    }

    /**
     * Set the new wand radius.
     *
     * @param newWandRadius the wandRadius to set
     */
    public static void setWandRadius(int newWandRadius) {
        if (newWandRadius < MINIMUM_WAND_RADIUS) {
            wandRadius = MINIMUM_WAND_RADIUS;
        }
        wandRadius = newWandRadius;
    }

    /**
     * Reset the wand cursor (must happen when its size is reset).
     */
    public void resetWandCursor() {
        cursorImage = new BufferedImage(wandRadius + 1, wandRadius + 1,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) cursorImage.getGraphics();

        // Draw stroke around wand
        int stroke = 1;
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(stroke));
        g2.drawOval(0, 0, wandRadius, wandRadius);

        // Draw wand itself
        g2.setColor(Color.YELLOW);
        g2.setStroke(new BasicStroke(1));
        float alpha;
        // Alpha levels do not display properly on modern Windows operating systems.
        // On Windows, values between >0.0f and 1.0f gives the color a different brightness, with 1.0f being the brightest.
        if (System.getProperty("os.name").startsWith("Windows")) {
        	alpha = 1.0f; // 0.0f is 100% transparent, 0.1f is almost 100% black, and 1.0f is 100% bright.
        } else {
        	alpha = .5f; // 0.0f is 100% transparent and 1.0f is 100% opaque.
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                alpha));
        g2.fillOval(0, 0, wandRadius, wandRadius);
        // g2.fillRect(0, 0, radius, radius);

        Toolkit tk = Toolkit.getDefaultToolkit();
        Cursor newCursor = tk.createCustomCursor(cursorImage, CENTER_POINT, "wand");
        this.cursor = newCursor;
    }

}