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
 *
 * @author  werni
 */
public class Splasher {
    /**
     * Shows the splash screen, launches the application and then disposes
     * the splash screen.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SplashWindow.splash(ResourceManager.getImage("simbrain-logo.gif"));
        SplashWindow.invokeMain("org.simbrain.workspace.Workspace", args);
        SplashWindow.disposeSplash();
    }
    
}
