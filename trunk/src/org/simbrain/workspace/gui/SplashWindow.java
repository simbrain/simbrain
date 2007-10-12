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
package org.simbrain.workspace.gui;


/*
 * @(#)SplashWindow.java  2.2  2005-04-03
 *
 * Copyright (c) 2003-2005 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is in the public domain.
 */
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * <b>SplashWindow</b>
 *
 * <p>
 * Usage: MyApplication is your application class. Create a Splasher class which opens the splash window, invokes the
 * main method of your Application class, and disposes the splash window afterwards. Please note that we want to keep
 * the Splasher class and the SplashWindow class as small as possible. The less code and the less classes must be
 * loaded into the JVM to open the splash screen, the faster it will appear.
 * <pre>
 * class Splasher {
 *    public static void main(String[] args) {
 *         SplashWindow.splash(Startup.class.getResource("splash.gif"));
 *         MyApplication.main(args);
 *         SplashWindow.disposeSplash();
 *    }
 * }
 * </pre>
 * </p>
 *
 * @author Werner Randelshofer
 * @version 2.1 2005-04-03 Revised.
 */
public final class SplashWindow extends Window {
    private static final long serialVersionUID = 1L;

    /** The current instance of the splash window. (Singleton design pattern). */
    private static SplashWindow instance;

    /** The splash image which is displayed on the splash window. */
    private Image image;

    /**
     * This attribute indicates whether the method paint(Graphics) has been called at least once since the construction
     * of this window.<br>
     * This attribute is used to notify method splash(Image) that the window has been drawn at least once by the AWT
     * event dispatcher thread.<br>
     * This attribute acts like a latch. Once set to true, it will never be changed back to false again.
     *
     * @see #paint
     * @see #splash
     */
    private boolean paintCalled = false;

    /**
     * Creates a new instance.
     *
     * @param parent the parent of the window.
     * @param image the splash image.
     */
    private SplashWindow(final Frame parent, final Image image) {
        super(parent);
        this.image = image;

        // Load the image
        MediaTracker mt = new MediaTracker(this);
        mt.addImage(image, 0);

        try {
            mt.waitForID(0);
        } catch (InterruptedException ie) {
        }

        // Center the window on the screen
        int imgWidth = image.getWidth(this);
        int imgHeight = image.getHeight(this);
        setSize(imgWidth, imgHeight);

        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenDim.width - imgWidth) / 2, (screenDim.height - imgHeight) / 2);

        // Users shall be able to close the splash window by
        // clicking on its display area. This mouse listener
        // listens for mouse clicks and disposes the splash window.
        MouseAdapter disposeOnClick = new MouseAdapter() {
                public void mouseClicked(final MouseEvent evt) {
                    // Note: To avoid that method splash hangs, we
                    // must set paintCalled to true and call notifyAll.
                    // This is necessary because the mouse click may
                    // occur before the contents of the window
                    // has been painted.
                    synchronized (SplashWindow.this) {
                        SplashWindow.this.paintCalled = true;
                        SplashWindow.this.notifyAll();
                    }

                    dispose();
                }
            };

        addMouseListener(disposeOnClick);
    }

    /**
     * Updates the display area of the window.
     *
     *  @param g Graphics to be updated
     */
    public void update(final Graphics g) {
        // Note: Since the paint method is going to draw an
        // image that covers the complete area of the component we
        // do not fill the component with its background color
        // here. This avoids flickering.
        paint(g);
    }

    /**
     * Paints the image on the window.
     *
     *  @param g Graphics to be painted
     */
    public void paint(final Graphics g) {
        g.drawImage(image, 0, 0, this);

        // Notify method splash that the window
        // has been painted.
        // Note: To improve performance we do not enter
        // the synchronized block unless we have to.
        if (!paintCalled) {
            paintCalled = true;

            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * Open's a splash window using the specified image.
     *
     * @param image The splash image.
     */
    public static void splash(final Image image) {
        if ((instance == null) && (image != null)) {
            Frame f = new Frame();

            // Create the splash image
            instance = new SplashWindow(f, image);

            // Show the window.
            instance.setVisible(true);

            // Note: To make sure the user gets a chance to see the
            // splash window we wait until its paint method has been
            // called at least once by the AWT event dispatcher thread.
            // If more than one processor is available, we don't wait,
            // and maximize CPU throughput instead.
            if (!EventQueue.isDispatchThread() && (Runtime.getRuntime().availableProcessors() == 1)) {
                synchronized (instance) {
                    while (!instance.paintCalled) {
                        try {
                            instance.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
    }

    /**
     * Closes the splash window.
     */
    public static void disposeSplash() {
        if (instance != null) {
            instance.getOwner().dispose();
            instance = null;
        }
    }

    /**
     * Invokes the main method of the provided class name.
     *
     * @param className Name of class to be invoked
     * @param args the command line arguments
     */
    public static void invokeMain(final String className, final String[] args) {
        try {
            Class.forName(className).getMethod("main", new Class[] {String[].class }).invoke(
                                                                                              null,
                                                                                              new Object[] {args });
        } catch (Exception e) {
            InternalError error = new InternalError("Failed to invoke main method");
            error.initCause(e);
            throw error;
        }
    }
}
