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
package org.simbrain.workspace;

import org.simbrain.resource.ResourceManager;


/*
 * @(#)Splasher.java  2.0  January 31, 2004
 *
 * Copyright (c) 2003-2004 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is in the public domain.
 */

/**
 * <b>Splasher</b> displays the simbrain splash screen an initializes the workspace.
 */
public class Splasher {
    /**
     * Shows the splash screen, launches the application and then disposes the splash screen.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        SplashWindow.splash(ResourceManager.getImage("simbrain-logo.gif"));
        SplashWindow.invokeMain("org.simbrain.workspace.Workspace", args);
        SplashWindow.disposeSplash();
    }
}
